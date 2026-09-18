package com.bana.dotori.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.report.dto.response.PointIncomeReportResponse;
import com.bana.dotori.report.dto.response.PointIncomeTypeAmount;
import com.bana.dotori.report.dto.response.PointSpendingReportResponse;
import com.bana.dotori.report.dto.response.PointSpendingTypeAmount;
import com.bana.dotori.report.dto.response.ReportDailyAmount;
import com.bana.dotori.report.service.AssetReportService;
import com.bana.dotori.report.service.PointReportService;
import com.bana.dotori.wallet.entity.PointTransactionReason;

/**
 * 컨트롤러 계층 - HTTP 경계만 검증한다 (RPT-02·03).
 *
 * <p>서비스를 가짜로 대체했으므로 집계는 전혀 일어나지 않는다. 여기가 실패하면 원인은
 * 요청 매핑, 쿼리 파라미터 바인딩, 예외 → 상태코드 매핑, 응답 JSON 모양 중 하나다.
 *
 * <p>팀 규칙대로 SecurityConfig 를 함께 올리되 필터는 꺼 둔다. 토큰을 발급할 수 없어
 * 필터를 켜면 모든 요청이 인증 단계에서 막혀 정작 보려는 매핑 검증에 도달하지 못한다
 * ({@code PointWalletControllerTest} 와 같은 이유).
 */
@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
@DisplayName("포인트 수입·소비 통계 API")
class ReportControllerTest {

	private static final String INCOME_URL = "/api/v1/families/{familyId}/children/{childId}/reports/income";
	private static final String SPENDING_URL = "/api/v1/families/{familyId}/children/{childId}/reports/spending";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PointReportService pointReportService;

	/** 같은 컨트롤러가 자산 조회(RPT-04)도 들고 있어 슬라이스에 필요하다. 검증은 AssetReportControllerTest 가 한다. */
	@MockitoBean
	private AssetReportService assetReportService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Nested
	@DisplayName("수입 통계 (RPT-02)")
	class 수입_통계 {

		@Test
		@DisplayName("조회에 성공하면 200 과 명세 형식의 응답을 돌려준다")
		void 수입_조회_성공() throws Exception {
			given(pointReportService.getIncomeReport(eq(10), eq(2), eq(1),
					eq(LocalDate.of(2026, 8, 1)), eq(LocalDate.of(2026, 8, 28)), isNull()))
					.willReturn(PointIncomeReportResponse.of(
							List.of(new PointIncomeTypeAmount(
											PointTransactionReason.ALLOWANCE, 12_000L, 4L),
									new PointIncomeTypeAmount(
											PointTransactionReason.CHORE_REWARD, 10_000L, 10L)),
							List.of(new ReportDailyAmount(LocalDate.of(2026, 8, 1), 3_000L))));

			mockMvc.perform(get(INCOME_URL, 10, 2).with(loginAs(1, "PARENT"))
							.param("startDate", "2026-08-01")
							.param("endDate", "2026-08-28"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.total").value(22_000))
					.andExpect(jsonPath("$.byType[0].incomeType").value("ALLOWANCE"))
					.andExpect(jsonPath("$.byType[0].amount").value(12_000))
					.andExpect(jsonPath("$.byType[0].count").value(4))
					.andExpect(jsonPath("$.byType[1].incomeType").value("CHORE_REWARD"))
					.andExpect(jsonPath("$.byPeriod[0].date").value("2026-08-01"))
					.andExpect(jsonPath("$.byPeriod[0].amount").value(3_000));
		}

		@Test
		@DisplayName("기간을 생략해도 호출된다 — 기본값은 서비스가 정한다")
		void 기간_없이도_호출된다() throws Exception {
			given(pointReportService.getIncomeReport(eq(10), eq(2), eq(1), isNull(), isNull(), isNull()))
					.willReturn(PointIncomeReportResponse.empty());

			mockMvc.perform(get(INCOME_URL, 10, 2).with(loginAs(1, "PARENT")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.total").value(0))
					.andExpect(jsonPath("$.byType").isEmpty())
					.andExpect(jsonPath("$.byPeriod").isEmpty());
		}

		@Test
		@DisplayName("incomeType 을 넘기면 그대로 서비스로 전달한다")
		void 유형_필터를_전달한다() throws Exception {
			given(pointReportService.getIncomeReport(eq(10), eq(2), eq(1),
					isNull(), isNull(), eq(PointTransactionReason.CHORE_REWARD)))
					.willReturn(PointIncomeReportResponse.empty());

			mockMvc.perform(get(INCOME_URL, 10, 2).with(loginAs(1, "PARENT"))
							.param("incomeType", "CHORE_REWARD"))
					.andExpect(status().isOk());

			verify(pointReportService).getIncomeReport(10, 2, 1,
					null, null, PointTransactionReason.CHORE_REWARD);
		}

		@Test
		@DisplayName("없는 incomeType 은 서비스까지 가지 않고 바인딩 단계에서 400 이다")
		void 없는_유형은_400이다() throws Exception {
			mockMvc.perform(get(INCOME_URL, 10, 2).with(loginAs(1, "PARENT"))
							.param("incomeType", "NOT_A_REASON"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

			verify(pointReportService, never())
					.getIncomeReport(any(), any(), any(), any(), any(), any());
		}

		@Test
		@DisplayName("날짜 형식이 틀리면 400 이다")
		void 날짜_형식_오류는_400이다() throws Exception {
			mockMvc.perform(get(INCOME_URL, 10, 2).with(loginAs(1, "PARENT"))
							.param("startDate", "2026/08/01"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

			verify(pointReportService, never())
					.getIncomeReport(any(), any(), any(), any(), any(), any());
		}

		@Test
		@DisplayName("남의 가족 자녀를 조회하면 403 NOT_FAMILY_CHILD 다")
		void 권한이_없으면_403이다() throws Exception {
			willThrow(new BusinessException(ErrorCode.NOT_FAMILY_CHILD))
					.given(pointReportService).getIncomeReport(eq(10), eq(2), eq(999),
							isNull(), isNull(), isNull());

			mockMvc.perform(get(INCOME_URL, 10, 2).with(loginAs(999, "PARENT")))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("NOT_FAMILY_CHILD"));
		}

		@Test
		@DisplayName("시작일이 종료일보다 늦으면 400 INVALID_DATE_RANGE 다")
		void 날짜_역전은_400이다() throws Exception {
			willThrow(new BusinessException(ErrorCode.INVALID_DATE_RANGE))
					.given(pointReportService).getIncomeReport(any(), any(), any(), any(), any(), any());

			mockMvc.perform(get(INCOME_URL, 10, 2).with(loginAs(1, "PARENT"))
							.param("startDate", "2026-08-28")
							.param("endDate", "2026-08-01"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
		}

		@Test
		@DisplayName("기간 상한을 넘으면 400 REPORT_PERIOD_TOO_LONG 이다")
		void 기간_상한_초과는_400이다() throws Exception {
			willThrow(new BusinessException(ErrorCode.REPORT_PERIOD_TOO_LONG))
					.given(pointReportService).getIncomeReport(any(), any(), any(), any(), any(), any());

			mockMvc.perform(get(INCOME_URL, 10, 2).with(loginAs(1, "PARENT"))
							.param("startDate", "2020-01-01")
							.param("endDate", "2026-08-01"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("REPORT_PERIOD_TOO_LONG"));
		}
	}

	@Nested
	@DisplayName("소비 통계 (RPT-03)")
	class 소비_통계 {

		@Test
		@DisplayName("조회에 성공하면 200 과 명세 형식의 응답을 돌려준다")
		void 소비_조회_성공() throws Exception {
			given(pointReportService.getSpendingReport(eq(10), eq(2), eq(1),
					eq(LocalDate.of(2026, 8, 1)), eq(LocalDate.of(2026, 8, 28))))
					.willReturn(PointSpendingReportResponse.of(
							List.of(new PointSpendingTypeAmount(
									PointTransactionReason.PURCHASE, 9_000L, 3L)),
							List.of(new ReportDailyAmount(LocalDate.of(2026, 8, 14), 3_000L))));

			mockMvc.perform(get(SPENDING_URL, 10, 2).with(loginAs(1, "PARENT"))
							.param("startDate", "2026-08-01")
							.param("endDate", "2026-08-28"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.total").value(9_000))
					.andExpect(jsonPath("$.byType[0].spendingType").value("PURCHASE"))
					.andExpect(jsonPath("$.byType[0].amount").value(9_000))
					.andExpect(jsonPath("$.byType[0].count").value(3))
					.andExpect(jsonPath("$.byPeriod[0].date").value("2026-08-14"))
					.andExpect(jsonPath("$.byPeriod[0].amount").value(3_000));
		}

		@Test
		@DisplayName("아이 본인도 자기 소비 통계를 조회할 수 있다")
		void 본인_조회도_열려_있다() throws Exception {
			given(pointReportService.getSpendingReport(eq(10), eq(2), eq(2), isNull(), isNull()))
					.willReturn(PointSpendingReportResponse.empty());

			mockMvc.perform(get(SPENDING_URL, 10, 2).with(loginAs(2, "CHILD")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.total").value(0));
		}

		@Test
		@DisplayName("소비 통계에는 유형 필터가 없다 — 파라미터를 줘도 무시된다")
		void 유형_필터는_받지_않는다() throws Exception {
			given(pointReportService.getSpendingReport(eq(10), eq(2), eq(1), isNull(), isNull()))
					.willReturn(PointSpendingReportResponse.empty());

			mockMvc.perform(get(SPENDING_URL, 10, 2).with(loginAs(1, "PARENT"))
							.param("spendingType", "PURCHASE"))
					.andExpect(status().isOk());

			verify(pointReportService).getSpendingReport(10, 2, 1, null, null);
		}

		@Test
		@DisplayName("남의 가족 자녀를 조회하면 403 NOT_FAMILY_CHILD 다")
		void 권한이_없으면_403이다() throws Exception {
			willThrow(new BusinessException(ErrorCode.NOT_FAMILY_CHILD))
					.given(pointReportService).getSpendingReport(any(), any(), any(), any(), any());

			mockMvc.perform(get(SPENDING_URL, 10, 2).with(loginAs(999, "PARENT")))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("NOT_FAMILY_CHILD"));
		}
	}

	/**
	 * 컨트롤러가 @AuthenticationPrincipal 로 읽는 주체를 심는다.
	 * 필터를 껐어도 인자 리졸버는 SecurityContext 를 보므로 이 방식이 필요하다.
	 */
	private static RequestPostProcessor loginAs(Integer userId, String role) {
		return authentication(new UsernamePasswordAuthenticationToken(
				userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
	}
}
