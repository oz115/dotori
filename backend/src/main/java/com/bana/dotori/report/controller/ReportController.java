package com.bana.dotori.report.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.report.dto.response.PointIncomeReportResponse;
import com.bana.dotori.report.dto.response.PointSpendingReportResponse;
import com.bana.dotori.report.service.PointReportService;
import com.bana.dotori.wallet.entity.PointTransactionReason;

import lombok.RequiredArgsConstructor;

/**
 * 리포트 API (RPT).
 *
 * <p>조회 권한 판정은 {@code ReportAccessValidator} 한 곳에 맡긴다. 아이 본인과 그 가족의
 * 부모가 모두 볼 수 있어서 컨트롤러에서는 본인 여부를 따지지 않는다 — 지갑 조회(WLT-04·06)와
 * 같은 구조다.
 *
 * <p>기간 파라미터를 생략하면 "이번 달 1일 ~ 오늘"(KST)로 본다. 일자는 모두 KST 기준이며,
 * 시작일과 종료일을 <b>양끝 포함</b>으로 해석한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/reports")
public class ReportController {

	private final PointReportService pointReportService;

	/**
	 * RPT-02 포인트 수입 통계 조회.
	 *
	 * <p>{@code incomeType} 은 enum 으로 받는다. 없는 값이 오면 스프링이 바인딩 단계에서
	 * 걸러 400 으로 응답하므로 서비스까지 내려가지 않는다. 다만 <b>존재하지만 수입이 아닌</b>
	 * 사유({@code PURCHASE} 등)는 바인딩을 통과하므로 서비스가 400 으로 막는다.
	 *
	 * <p>필터를 주면 {@code total}·{@code byType}·{@code byPeriod} 가 모두 그 사유 기준이 된다.
	 *
	 * @param startDate  조회 시작일(포함). 생략하면 이번 달 1일
	 * @param endDate    조회 종료일(포함). 생략하면 오늘
	 * @param incomeType 수입 사유 필터. 생략하면 수입 전체
	 */
	@GetMapping("/income")
	public PointIncomeReportResponse getIncomeReport(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate,
			@RequestParam(required = false) PointTransactionReason incomeType) {

		return pointReportService.getIncomeReport(
				familyId, childId, loginUserId, startDate, endDate, incomeType);
	}

	/**
	 * RPT-03 포인트 소비 통계 조회.
	 *
	 * <p>명세에 유형 필터가 없어 받지 않는다.
	 *
	 * @param startDate 조회 시작일(포함). 생략하면 이번 달 1일
	 * @param endDate   조회 종료일(포함). 생략하면 오늘
	 */
	@GetMapping("/spending")
	public PointSpendingReportResponse getSpendingReport(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate) {

		return pointReportService.getSpendingReport(
				familyId, childId, loginUserId, startDate, endDate);
	}
}
