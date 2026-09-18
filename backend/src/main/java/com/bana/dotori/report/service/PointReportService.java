package com.bana.dotori.report.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.report.dto.ReportTypeAmountRow;
import com.bana.dotori.report.dto.response.PointIncomeReportResponse;
import com.bana.dotori.report.dto.response.PointIncomeTypeAmount;
import com.bana.dotori.report.dto.response.PointSpendingReportResponse;
import com.bana.dotori.report.dto.response.PointSpendingTypeAmount;
import com.bana.dotori.report.dto.response.ReportDailyAmount;
import com.bana.dotori.report.repository.PointReportQueryRepository;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointTransactionType;

import lombok.RequiredArgsConstructor;

/**
 * 포인트 수입·소비 통계 (RPT-02·03).
 *
 * <p>두 기능이 방향만 다르고 나머지가 같다. 기간 해석, 권한 확인, 집계, 유형 필터가
 * 전부 겹치므로 {@link #aggregate} 하나로 묶고 방향과 응답 조립만 갈라 붙인다.
 * 따로 쓰면 한쪽만 고쳐져 수입과 소비가 다른 기간 규칙을 갖게 된다.
 */
@Service
@RequiredArgsConstructor
public class PointReportService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/**
	 * 한 번에 조회할 수 있는 기간의 상한(일).
	 *
	 * <p>{@code byPeriod} 가 일자마다 한 줄이라 상한이 없으면 10 년을 요청해 3650 줄을
	 * 한 번에 끌어갈 수 있다. 윤년을 포함한 1 년을 허용하려고 366 으로 잡았다.
	 */
	static final int MAX_PERIOD_DAYS = 366;

	private final PointReportQueryRepository pointReportQueryRepository;
	private final ReportAccessValidator reportAccessValidator;

	/**
	 * 오늘을 어디서 얻는지 주입받는다. 기간 기본값이 "이번 달 1일 ~ 오늘"이라
	 * {@code LocalDate.now()} 를 직접 부르면 테스트가 실행 날짜에 따라 달라진다.
	 */
	private final Clock clock;

	/**
	 * RPT-02 포인트 수입 통계 조회.
	 *
	 * @param startDate  조회 시작일(KST, 포함). null 이면 이번 달 1일
	 * @param endDate    조회 종료일(KST, 포함). null 이면 오늘
	 * @param incomeType 수입 사유 필터. null 이면 수입 사유 전체
	 */
	@Transactional(readOnly = true)
	public PointIncomeReportResponse getIncomeReport(Integer familyId, Integer childId, Integer loginUserId,
			LocalDate startDate, LocalDate endDate, PointTransactionReason incomeType) {

		Aggregated aggregated = aggregate(familyId, childId, loginUserId,
				startDate, endDate, incomeType, PointTransactionType.DEPOSIT);

		if (aggregated.byType().isEmpty()) {
			return PointIncomeReportResponse.empty();
		}

		return PointIncomeReportResponse.of(
				aggregated.byType().stream().map(PointIncomeTypeAmount::from).toList(),
				aggregated.byPeriod());
	}

	/**
	 * RPT-03 포인트 소비 통계 조회.
	 *
	 * <p>명세에 유형 필터가 없어 받지 않는다. 필요해지면 수입과 같은 방식으로 열면 된다.
	 *
	 * @param startDate 조회 시작일(KST, 포함). null 이면 이번 달 1일
	 * @param endDate   조회 종료일(KST, 포함). null 이면 오늘
	 */
	@Transactional(readOnly = true)
	public PointSpendingReportResponse getSpendingReport(Integer familyId, Integer childId, Integer loginUserId,
			LocalDate startDate, LocalDate endDate) {

		Aggregated aggregated = aggregate(familyId, childId, loginUserId,
				startDate, endDate, null, PointTransactionType.WITHDRAWAL);

		if (aggregated.byType().isEmpty()) {
			return PointSpendingReportResponse.empty();
		}

		return PointSpendingReportResponse.of(
				aggregated.byType().stream().map(PointSpendingTypeAmount::from).toList(),
				aggregated.byPeriod());
	}

	/**
	 * 수입·소비가 공유하는 집계 본체.
	 *
	 * <p>필터를 주면 {@code byType}·{@code byPeriod}·{@code total} 이 <b>모두</b> 그 사유만
	 * 기준이 된다. total 만 전체로 두면 byType 의 합과 total 이 어긋나 화면에서 비율이 안 맞는다.
	 */
	private Aggregated aggregate(Integer familyId, Integer childId, Integer loginUserId,
			LocalDate startDate, LocalDate endDate, PointTransactionReason filter,
			PointTransactionType direction) {

		reportAccessValidator.validate(familyId, childId, loginUserId);

		LocalDate from = startDate == null ? firstDayOfThisMonth() : startDate;
		LocalDate to = endDate == null ? today() : endDate;
		validatePeriod(from, to);

		Collection<PointTransactionReason> reasons = resolveReasons(direction, filter);
		OffsetDateTime startAt = startOfDay(from);
		OffsetDateTime endAt = startOfNextDay(to);

		List<ReportTypeAmountRow> byType =
				pointReportQueryRepository.aggregateByReason(childId, startAt, endAt, reasons);

		if (byType.isEmpty()) {
			// 사유별 집계가 비었으면 일자별도 반드시 비어 있다. 쿼리를 한 번 아낀다.
			return new Aggregated(List.of(), List.of());
		}

		List<ReportDailyAmount> byPeriod =
				pointReportQueryRepository.aggregateByDate(childId, startAt, endAt, reasons);

		return new Aggregated(byType, byPeriod);
	}

	/**
	 * 집계 대상 사유를 정한다.
	 *
	 * <p>필터가 없으면 그 방향의 사유 전체다. 필터가 있으면 그 사유 하나인데,
	 * <b>방향이 맞는지 확인한다.</b> 수입 통계에 {@code PURCHASE} 를 넘기면 조건상 0 건이
	 * 나와 "그 기간에 수입이 없다"로 보이는데, 실제로는 물어본 것이 잘못된 질문이다.
	 * 빈 통계로 조용히 넘기면 클라이언트가 파라미터 오류를 눈치챌 방법이 없다.
	 */
	private Collection<PointTransactionReason> resolveReasons(PointTransactionType direction,
			PointTransactionReason filter) {

		if (filter == null) {
			return direction == PointTransactionType.DEPOSIT
					? PointTransactionReason.depositReasons()
					: PointTransactionReason.withdrawalReasons();
		}

		if (!filter.isAllowedIn(direction)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST,
					"이 통계에서 쓸 수 없는 사유입니다. reason=" + filter + ", direction=" + direction);
		}
		return List.of(filter);
	}

	private void validatePeriod(LocalDate startDate, LocalDate endDate) {
		if (startDate.isAfter(endDate)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE,
					"조회 시작일이 종료일보다 늦습니다. startDate=" + startDate + ", endDate=" + endDate);
		}

		// 양끝을 포함하므로 하루 조회의 일수는 1 이다.
		long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
		if (days > MAX_PERIOD_DAYS) {
			throw new BusinessException(ErrorCode.REPORT_PERIOD_TOO_LONG,
					"조회 기간이 상한을 넘었습니다. days=" + days + ", 허용=" + MAX_PERIOD_DAYS);
		}
	}

	private LocalDate today() {
		return LocalDate.now(clock.withZone(KST));
	}

	private LocalDate firstDayOfThisMonth() {
		return today().withDayOfMonth(1);
	}

	/** 조회 시작일 00:00:00 KST. */
	private OffsetDateTime startOfDay(LocalDate date) {
		return date.atStartOfDay(KST).toOffsetDateTime();
	}

	/**
	 * 조회 종료일 다음 날 00:00:00 KST.
	 *
	 * <p>종료일을 포함하려면 반개구간의 끝을 다음 날 0 시로 잡아야 한다.
	 * "종료일 23:59:59" 로 자르면 그 뒤 밀리초에 생긴 거래가 빠진다.
	 */
	private OffsetDateTime startOfNextDay(LocalDate date) {
		return date.plusDays(1).atStartOfDay(KST).toOffsetDateTime();
	}

	/** 두 집계 결과를 함께 들고 다니기 위한 내부 묶음. */
	private record Aggregated(
		List<ReportTypeAmountRow> byType,
		List<ReportDailyAmount> byPeriod
	) {
	}
}
