package com.bana.dotori.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
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
import com.bana.dotori.report.dto.response.AssetReportResponse;
import com.bana.dotori.report.dto.response.AssetReportResponse.InvestmentAsset;
import com.bana.dotori.report.dto.response.AssetReportResponse.PointAsset;
import com.bana.dotori.report.dto.response.AssetReportResponse.SavingsAsset;
import com.bana.dotori.report.service.AssetReportService;
import com.bana.dotori.report.service.PointReportService;

/**
 * 컨트롤러 계층 - 자산 현황 조회의 HTTP 경계만 검증한다 (RPT-04).
 *
 * <p>수입·소비({@link ReportControllerTest})와 같은 컨트롤러지만 테스트를 나눴다. 저쪽은
 * 기간·필터 바인딩이 검증 대상이고 여기는 응답 JSON 모양이다 — 한 클래스에 두면 어느
 * 엔드포인트가 깨졌는지 이름만으로 알 수 없다.
 */
@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
@DisplayName("자산 현황 조회 API")
class AssetReportControllerTest {

	private static final String ASSETS_URL = "/api/v1/families/{familyId}/children/{childId}/reports/assets";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AssetReportService assetReportService;

	@MockitoBean
	private PointReportService pointReportService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("조회에 성공하면 200 과 명세 형식의 응답을 돌려준다")
	void 자산_조회_성공() throws Exception {
		given(assetReportService.getAssetReport(eq(10), eq(2), eq(1)))
				.willReturn(AssetReportResponse.of(2,
						new PointAsset(20_000),
						new SavingsAsset(15_000, 1),
						new InvestmentAsset(6_000L, 1_000L)));

		mockMvc.perform(get(ASSETS_URL, 10, 2).with(loginAs(1, "PARENT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.childId").value(2))
				.andExpect(jsonPath("$.totalAssets").value(41_000))
				.andExpect(jsonPath("$.point.balance").value(20_000))
				.andExpect(jsonPath("$.savings.totalAmount").value(15_000))
				.andExpect(jsonPath("$.savings.activeCount").value(1))
				.andExpect(jsonPath("$.investment.valuation").value(6_000))
				.andExpect(jsonPath("$.investment.profitLoss").value(1_000));
	}

	@Test
	@DisplayName("수익률 필드는 내보내지 않는다")
	void 수익률은_응답에_없다() throws Exception {
		given(assetReportService.getAssetReport(any(), any(), any()))
				.willReturn(AssetReportResponse.of(2,
						new PointAsset(0), new SavingsAsset(0, 0), new InvestmentAsset(6_000L, 1_000L)));

		mockMvc.perform(get(ASSETS_URL, 10, 2).with(loginAs(1, "PARENT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.investment.profitLossRate").doesNotExist());
	}

	@Test
	@DisplayName("기간 파라미터를 줘도 무시한다 — 현재 시점 스냅샷이다")
	void 기간_파라미터는_받지_않는다() throws Exception {
		given(assetReportService.getAssetReport(eq(10), eq(2), eq(1)))
				.willReturn(emptyAssets());

		mockMvc.perform(get(ASSETS_URL, 10, 2).with(loginAs(1, "PARENT"))
						.param("startDate", "2026-08-01")
						.param("endDate", "2026-08-28"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("아이 본인도 자기 자산을 조회할 수 있다")
	void 본인_조회도_열려_있다() throws Exception {
		given(assetReportService.getAssetReport(eq(10), eq(2), eq(2)))
				.willReturn(emptyAssets());

		mockMvc.perform(get(ASSETS_URL, 10, 2).with(loginAs(2, "CHILD")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAssets").value(0));
	}

	@Test
	@DisplayName("자산이 없어도 404 가 아니라 0 으로 200 이다")
	void 자산이_없으면_0으로_200이다() throws Exception {
		given(assetReportService.getAssetReport(any(), any(), any())).willReturn(emptyAssets());

		mockMvc.perform(get(ASSETS_URL, 10, 2).with(loginAs(1, "PARENT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAssets").value(0))
				.andExpect(jsonPath("$.point.balance").value(0))
				.andExpect(jsonPath("$.savings.activeCount").value(0))
				.andExpect(jsonPath("$.investment.valuation").value(0));
	}

	@Test
	@DisplayName("남의 가족 자녀를 조회하면 403 NOT_FAMILY_CHILD 다")
	void 권한이_없으면_403이다() throws Exception {
		willThrow(new BusinessException(ErrorCode.NOT_FAMILY_CHILD))
				.given(assetReportService).getAssetReport(any(), any(), any());

		mockMvc.perform(get(ASSETS_URL, 10, 2).with(loginAs(999, "PARENT")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_FAMILY_CHILD"));
	}

	@Test
	@DisplayName("손실이면 손익이 음수로 나간다")
	void 손실은_음수로_나간다() throws Exception {
		given(assetReportService.getAssetReport(any(), any(), any()))
				.willReturn(AssetReportResponse.of(2,
						new PointAsset(0), new SavingsAsset(0, 0), new InvestmentAsset(8_000L, -2_000L)));

		mockMvc.perform(get(ASSETS_URL, 10, 2).with(loginAs(1, "PARENT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.investment.profitLoss").value(-2_000));
	}

	private static AssetReportResponse emptyAssets() {
		return AssetReportResponse.of(2,
				new PointAsset(0), new SavingsAsset(0, 0), new InvestmentAsset(0L, 0L));
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
