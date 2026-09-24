package com.bana.dotori.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.report.dto.ReportTypeAmountRow;
import com.bana.dotori.report.dto.response.PointIncomeReportResponse;
import com.bana.dotori.report.dto.response.PointIncomeTypeAmount;
import com.bana.dotori.report.dto.response.PointSpendingReportResponse;
import com.bana.dotori.report.dto.response.ReportDailyAmount;
import com.bana.dotori.report.repository.PointReportQueryRepository;
import com.bana.dotori.wallet.entity.PointTransactionReason;

/**
 * 서비스 계층 - 기간 해석, 방향별 사유 집합, 필터 규칙을 검증한다.
 *
 * <p>DB 를 띄우지 않으므로 집계 결과는 가짜다. 여기가 실패하면 원인은 "어떤 조건으로
 * 리포지토리를 부르는가"와 "결과를 어떻게 조립하는가" 중 하나다. KST 날짜 환산이
 * 실제로 맞는지는 {@code PointReportQueryRepositoryTest} 가 DB 를 띄워 확인한다.
 *
 * <p>서비스를 {@code @InjectMocks} 대신 직접 만든다. {@link Clock} 은 가짜로 바꿀 대상이
 * 아니라 <b>고정값을 넣어야 하는</b> 협력자라서, 주입을 맡기면 의도가 흐려진다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("포인트 수입·소비 통계 서비스")
class PointReportServiceTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final Integer PARENT_ID = 1;
	private static final Integer STRANGER_ID = 999;

	/** "오늘"을 2026-08-28 (KST) 로 고정한다. 기간 기본값이 실행 날짜에 흔들리지 않게 한다. */
	private static final Clock FIXED_CLOCK = Clock.fixed(
			OffsetDateTime.parse("2026-08-28T13:00:00+09:00").toInstant(), ZoneId.of("Asia/Seoul"));

	@Mock
	private PointReportQueryRepository pointReportQueryRepository;

	@Mock
	private ReportAccessValidator reportAccessValidator;

	@Captor
	private ArgumentCaptor<OffsetDateTime> startAtCaptor;

	@Captor
	private ArgumentCaptor<OffsetDateTime> endAtCaptor;

	@Captor
	private ArgumentCaptor<Collection<PointTransactionReason>> reasonsCaptor;

	private PointReportService pointReportService;

	@BeforeEach
	void setUp() {
		pointReportService = new PointReportService(
				pointReportQueryRepository, reportAccessValidator, FIXED_CLOCK);
	}

	@Nested
	@DisplayName("권한")
	class 권한 {

		@Test
		@DisplayName("권한 검증이 막으면 집계를 하지 않는다")
		void 권한_없으면_집계하지_않는다() {
			denyAccess();

			assertThatThrownBy(() -> pointReportService.getIncomeReport(
					FAMILY_ID, CHILD_ID, STRANGER_ID, null, null, null))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException)e).getErrorCode())
					.isEqualTo(ErrorCode.NOT_FAMILY_CHILD);

			verify(pointReportQueryRepository, never())
					.aggregateByReason(any(), any(), any(), anyCollection());
		}

		@Test
		@DisplayName("소비 통계도 같은 검증을 거친다")
		void 소비도_권한을_본다() {
			denyAccess();

			assertThatThrownBy(() -> pointReportService.getSpendingReport(
					FAMILY_ID, CHILD_ID, STRANGER_ID, null, null))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException)e).getErrorCode())
					.isEqualTo(ErrorCode.NOT_FAMILY_CHILD);

			verify(pointReportQueryRepository, never())
					.aggregateByReason(any(), any(), any(), anyCollection());
		}

		private void denyAccess() {
			willThrow(new BusinessException(ErrorCode.NOT_FAMILY_CHILD))
					.given(reportAccessValidator).validate(FAMILY_ID, CHILD_ID, STRANGER_ID);
		}
	}

	@Nested
	@DisplayName("기간")
	class 기간 {

		@Test
		@DisplayName("기간을 생략하면 이번 달 1일부터 오늘까지를 본다")
		void 기본_기간은_이번달_1일부터_오늘까지다() {
			givenAggregated(row(PointTransactionReason.ALLOWANCE, 12_000L, 4L));

			pointReportService.getIncomeReport(FAMILY_ID, CHILD_ID, PARENT_ID, null, null, null);

			captureReasonQuery();
			assertThat(startAtCaptor.getValue())
					.isEqualTo(OffsetDateTime.parse("2026-08-01T00:00:00+09:00"));
			// 종료일(8/28)을 포함하려면 끝은 다음 날 0 시여야 한다.
			assertThat(endAtCaptor.getValue())
					.isEqualTo(OffsetDateTime.parse("2026-08-29T00:00:00+09:00"));
		}

		@Test
		@DisplayName("종료일을 포함하도록 다음 날 0시까지로 넓혀서 조회한다")
		void 종료일을_포함한다() {
			givenAggregated(row(PointTransactionReason.GRANT, 1_000L, 1L));

			pointReportService.getIncomeReport(FAMILY_ID, CHILD_ID, PARENT_ID,
					LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14), null);

			captureReasonQuery();
			assertThat(endAtCaptor.getValue())
					.isEqualTo(OffsetDateTime.parse("2026-08-15T00:00:00+09:00"));
		}

		@Test
		@DisplayName("시작일이 종료일보다 늦으면 400 INVALID_DATE_RANGE 로 막는다")
		void 날짜가_역전되면_막는다() {
			assertThatThrownBy(() -> pointReportService.getIncomeReport(FAMILY_ID, CHILD_ID, PARENT_ID,
					LocalDate.of(2026, 8, 28), LocalDate.of(2026, 8, 1), null))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException)e).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_DATE_RANGE);

			verify(pointReportQueryRepository, never())
					.aggregateByReason(any(), any(), any(), anyCollection());
		}

		@Test
		@DisplayName("같은 날 하루만 조회하는 것은 허용한다")
		void 하루만_조회할_수_있다() {
			givenAggregated(row(PointTransactionReason.QUIZ_REWARD, 500L, 1L));

			pointReportService.getIncomeReport(FAMILY_ID, CHILD_ID, PARENT_ID,
					LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 14), null);

			captureReasonQuery();
			assertThat(startAtCaptor.getValue())
					.isEqualTo(OffsetDateTime.parse("2026-08-14T00:00:00+09:00"));
			assertThat(endAtCaptor.getValue())
					.isEqualTo(OffsetDateTime.parse("2026-08-15T00:00:00+09:00"));
		}

		@Test
		@DisplayName("상한인 366일까지는 허용한다")
		void 상한까지는_허용한다() {
			givenAggregated(row(PointTransactionReason.CHARGE, 1_000L, 1L));
			LocalDate start = LocalDate.of(2026, 1, 1);

			pointReportService.getIncomeReport(FAMILY_ID, CHILD_ID, PARENT_ID,
					start, start.plusDays(365), null);

			verify(pointReportQueryRepository)
					.aggregateByReason(any(), any(), any(), anyCollection());
		}

		@Test
		@DisplayName("367일을 요청하면 400 REPORT_PERIOD_TOO_LONG 으로 막는다")
		void 기간_상한을_넘으면_막는다() {
			LocalDate start = LocalDate.of(2026, 1, 1);

			assertThatThrownBy(() -> pointReportService.getIncomeReport(FAMILY_ID, CHILD_ID, PARENT_ID,
					start, start.plusDays(366), null))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException)e).getErrorCode())
					.isEqualTo(ErrorCode.REPORT_PERIOD_TOO_LONG);

			verify(pointReportQueryRepository, never())
					.aggregateByReason(any(), any(), any(), anyCollection());
		}
	}

	@Nested
	@DisplayName("집계 대상 사유")
	class 집계_대상_사유 {

		@Test
		@DisplayName("수입 통계는 포인트가 늘어나는 사유 8개만 집계한다")
		void 수입은_입금_사유만_본다() {
			givenAggregated(row(PointTransactionReason.ALLOWANCE, 12_000L, 4L));

			pointReportService.getIncomeReport(FAMILY_ID, CHILD_ID, PARENT_ID, null, null, null);

			captureReasonQuery();
			assertThat(reasonsCaptor.getValue()).containsExactlyInAnyOrder(
					PointTransactionReason.CHARGE,
					PointTransactionReason.GRANT,
					PointTransactionReason.ALLOWANCE,
					PointTransactionReason.CHORE_REWARD,
					PointTransactionReason.QUIZ_REWARD,
					PointTransactionReason.SAVINGS_WITHDRAWAL,
					PointTransactionReason.INVESTMENT_SELL,
					PointTransactionReason.REFUND);
		}

		@Test
		@DisplayName("소비 통계는 포인트가 줄어드는 사유 4개만 집계한다")
		void 소비는_출금_사유만_본다() {
			givenAggregated(row(PointTransactionReason.PURCHASE, 9_000L, 3L));

			pointReportService.getSpendingReport(FAMILY_ID, CHILD_ID, PARENT_ID, null, null);

			captureReasonQuery();
			assertThat(reasonsCaptor.getValue()).containsExactlyInAnyOrder(
					PointTransactionReason.PURCHASE,
					PointTransactionReason.CASHOUT,
					PointTransactionReason.SAVINGS_DEPOSIT,
					PointTransactionReason.INVESTMENT_BUY);
		}

		@Test
		@DisplayName("수입과 소비는 서로의 사유를 섞지 않는다")
		void 수입과_소비_사유는_겹치지_않는다() {
			assertThat(PointTransactionReason.depositReasons())
					.doesNotContainAnyElementsOf(PointTransactionReason.withdrawalReasons());
			// 12 개 사유가 두 방향으로 빠짐없이 갈린다. 새 사유가 어느 쪽에도 안 들어가면 여기서 깨진다.
			assertThat(PointTransactionReason.depositReasons().size()
					+ PointTransactionReason.withdrawalReasons().size())
					.isEqualTo(PointTransactionReason.values().length);
		}

		@Test
		@DisplayName("수입 사유 하나로 필터하면 그 사유만 집계한다")
		void 필터를_주면_그_사유만_본다() {
			givenAggregated(row(PointTransactionReason.CHORE_REWARD, 10_000L, 10L));

			pointReportService.getIncomeReport(FAMILY_ID, CHILD_ID, PARENT_ID,
					null, null, PointTransactionReason.CHORE_REWARD);

			captureReasonQuery();
			assertThat(reasonsCaptor.getValue())
					.containsExactly(PointTransactionReason.CHORE_REWARD);
		}

		@Test
		@DisplayName("수입 통계에 소비 사유를 넘기면 빈 통계가 아니라 400 으로 막는다")
		void 방향이_안_맞는_필터는_400이다() {
			assertThatThrownBy(() -> pointReportService.getIncomeReport(FAMILY_ID, CHILD_ID, PARENT_ID,
					null, null, PointTransactionReason.PURCHASE))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException)e).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_REQUEST);

			verify(pointReportQueryRepository, never())
					.aggregateByReason(any(), any(), any(), anyCollection());
		}
	}

	@Nested
	@DisplayName("응답 조립")
	class 응답_조립 {

		@Test
		@DisplayName("total 은 byType 의 합이다")
		void total은_byType의_합이다() {
			givenAggregated(
					row(PointTransactionReason.ALLOWANCE, 12_000L, 4L),
					row(PointTransactionReason.CHORE_REWARD, 10_000L, 10L),
					row(PointTransactionReason.QUIZ_REWARD, 3_000L, 6L));

			PointIncomeReportResponse response = pointReportService.getIncomeReport(
					FAMILY_ID, CHILD_ID, PARENT_ID, null, null, null);

			assertThat(response.total()).isEqualTo(25_000L);
			assertThat(response.byType()).hasSize(3);
			assertThat(response.byType().get(0).incomeType())
					.isEqualTo(PointTransactionReason.ALLOWANCE);
			assertThat(response.byType().get(0).count()).isEqualTo(4L);
		}

		@Test
		@DisplayName("REFUND 도 수입으로 집계되어 total 에 들어간다")
		void 환불도_수입이다() {
			givenAggregated(
					row(PointTransactionReason.GRANT, 1_000L, 1L),
					row(PointTransactionReason.REFUND, 500L, 1L));

			PointIncomeReportResponse response = pointReportService.getIncomeReport(
					FAMILY_ID, CHILD_ID, PARENT_ID, null, null, null);

			assertThat(response.total()).isEqualTo(1_500L);
			assertThat(response.byType())
					.extracting(PointIncomeTypeAmount::incomeType)
					.contains(PointTransactionReason.REFUND);
		}

		@Test
		@DisplayName("소비 응답은 spendingType 으로 내보낸다")
		void 소비_응답_조립() {
			givenAggregated(row(PointTransactionReason.SAVINGS_DEPOSIT, 15_000L, 2L));

			PointSpendingReportResponse response = pointReportService.getSpendingReport(
					FAMILY_ID, CHILD_ID, PARENT_ID, null, null);

			assertThat(response.total()).isEqualTo(15_000L);
			assertThat(response.byType().get(0).spendingType())
					.isEqualTo(PointTransactionReason.SAVINGS_DEPOSIT);
		}

		@Test
		@DisplayName("거래가 없으면 0 과 빈 목록으로 응답하고 일자별 집계는 부르지 않는다")
		void 거래가_없으면_빈_통계다() {
			given(pointReportQueryRepository.aggregateByReason(any(), any(), any(), anyCollection()))
					.willReturn(List.of());

			PointIncomeReportResponse response = pointReportService.getIncomeReport(
					FAMILY_ID, CHILD_ID, PARENT_ID, null, null, null);

			assertThat(response.total()).isZero();
			assertThat(response.byType()).isEmpty();
			assertThat(response.byPeriod()).isEmpty();

			// 사유별이 비었으면 일자별도 반드시 비어 있다. 쿼리를 아낀다.
			verify(pointReportQueryRepository, never())
					.aggregateByDate(any(), any(), any(), anyCollection());
		}

		@Test
		@DisplayName("일자별 집계를 그대로 실어 보낸다")
		void 일자별_집계를_그대로_내보낸다() {
			givenAggregated(row(PointTransactionReason.ALLOWANCE, 3_000L, 1L));
			given(pointReportQueryRepository.aggregateByDate(any(), any(), any(), anyCollection()))
					.willReturn(List.of(new ReportDailyAmount(LocalDate.of(2026, 8, 1), 3_000L)));

			PointIncomeReportResponse response = pointReportService.getIncomeReport(
					FAMILY_ID, CHILD_ID, PARENT_ID, null, null, null);

			assertThat(response.byPeriod()).hasSize(1);
			assertThat(response.byPeriod().get(0).date()).isEqualTo(LocalDate.of(2026, 8, 1));
			assertThat(response.byPeriod().get(0).amount()).isEqualTo(3_000L);
		}
	}

	private void captureReasonQuery() {
		verify(pointReportQueryRepository).aggregateByReason(eq(CHILD_ID),
				startAtCaptor.capture(), endAtCaptor.capture(), reasonsCaptor.capture());
	}

	private void givenAggregated(ReportTypeAmountRow... rows) {
		given(pointReportQueryRepository.aggregateByReason(any(), any(), any(), anyCollection()))
				.willReturn(List.of(rows));
		given(pointReportQueryRepository.aggregateByDate(any(), any(), any(), anyCollection()))
				.willReturn(List.of());
	}

	private ReportTypeAmountRow row(PointTransactionReason reason, Long amount, Long count) {
		return new ReportTypeAmountRow(reason, amount, count);
	}
}
