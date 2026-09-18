package com.bana.dotori.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentHoldingSummary;
import com.bana.dotori.investment.dto.InvestmentHoldingsResponse;
import com.bana.dotori.investment.service.InvestmentProductService;
import com.bana.dotori.report.dto.response.AssetReportResponse;
import com.bana.dotori.savings.dto.response.SavingsSummaryResponse;
import com.bana.dotori.savings.service.SavingsService;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointWalletRepository;

/**
 * 서비스 계층 - 세 도메인을 모아 자산으로 합치는 규칙을 검증한다 (RPT-04).
 *
 * <p>협력자를 전부 가짜로 대체했으므로 실제 잔액·평가액 조회는 일어나지 않는다. 여기가
 * 실패하면 원인은 "세 값을 어떻게 더하는가"와 "어떤 집합을 세는가" 중 하나다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("자산 현황 조회 서비스")
class AssetReportServiceTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final Integer PARENT_ID = 1;
	private static final Integer STRANGER_ID = 999;

	@Mock
	private PointWalletRepository pointWalletRepository;

	@Mock
	private SavingsService savingsService;

	@Mock
	private InvestmentProductService investmentProductService;

	@Mock
	private ReportAccessValidator reportAccessValidator;

	private AssetReportService assetReportService;

	@BeforeEach
	void setUp() {
		assetReportService = new AssetReportService(pointWalletRepository, savingsService,
				investmentProductService, reportAccessValidator);
		givenPoint(0);
		givenSavings(0, 0, 0);
		givenHoldings();
	}

	@Nested
	@DisplayName("권한")
	class 권한 {

		@Test
		@DisplayName("권한 검증이 막으면 어떤 자산도 읽지 않는다")
		void 권한_없으면_조회하지_않는다() {
			willThrow(new BusinessException(ErrorCode.NOT_FAMILY_CHILD))
					.given(reportAccessValidator).validate(FAMILY_ID, CHILD_ID, STRANGER_ID);

			assertThatThrownBy(() -> assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, STRANGER_ID))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException)e).getErrorCode())
					.isEqualTo(ErrorCode.NOT_FAMILY_CHILD);

			verify(pointWalletRepository, never()).findByChildId(any());
			verify(savingsService, never()).getSummary(any(), any(), any());
			verify(investmentProductService, never()).getHoldings(any(), any());
		}

		@Test
		@DisplayName("권한 검증을 다른 도메인 조회보다 먼저 한다")
		void 권한을_먼저_본다() {
			assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			org.mockito.InOrder order = org.mockito.Mockito.inOrder(
					reportAccessValidator, savingsService, investmentProductService);
			order.verify(reportAccessValidator).validate(FAMILY_ID, CHILD_ID, PARENT_ID);
			order.verify(savingsService).getSummary(PARENT_ID, FAMILY_ID, CHILD_ID);
			order.verify(investmentProductService).getHoldings(PARENT_ID, CHILD_ID);
		}
	}

	@Nested
	@DisplayName("총자산 합산")
	class 총자산_합산 {

		@Test
		@DisplayName("포인트 + 예적금 + 투자 평가액을 더한다")
		void 세_자산을_더한다() {
			givenPoint(20_000);
			givenSavings(15_000, 1, 0);
			givenHoldings(holding(1, 5_000, 6_000));

			AssetReportResponse response =
					assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.childId()).isEqualTo(CHILD_ID);
			assertThat(response.point().balance()).isEqualTo(20_000);
			assertThat(response.savings().totalAmount()).isEqualTo(15_000);
			assertThat(response.investment().valuation()).isEqualTo(6_000L);
			assertThat(response.totalAssets()).isEqualTo(41_000L);
		}

		@Test
		@DisplayName("투자 평가 손익은 총자산에 따로 더하지 않는다 — 평가액에 이미 반영돼 있다")
		void 손익을_중복으로_더하지_않는다() {
			givenPoint(0);
			givenSavings(0, 0, 0);
			givenHoldings(holding(1, 5_000, 6_000));

			AssetReportResponse response =
					assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.investment().profitLoss()).isEqualTo(1_000L);
			// 6000(평가액)만 총자산이다. 1000(손익)을 또 더해 7000 이 되면 안 된다.
			assertThat(response.totalAssets()).isEqualTo(6_000L);
		}

		@Test
		@DisplayName("자산이 전혀 없으면 전부 0 이다 — 오류가 아니다")
		void 자산이_없으면_0이다() {
			AssetReportResponse response =
					assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.totalAssets()).isZero();
			assertThat(response.point().balance()).isZero();
			assertThat(response.savings().totalAmount()).isZero();
			assertThat(response.savings().activeCount()).isZero();
			assertThat(response.investment().valuation()).isZero();
			assertThat(response.investment().profitLoss()).isZero();
		}
	}

	@Nested
	@DisplayName("포인트")
	class 포인트 {

		@Test
		@DisplayName("지갑이 아직 없으면 잔액 0 으로 본다")
		void 지갑이_없으면_0이다() {
			given(pointWalletRepository.findByChildId(CHILD_ID)).willReturn(Optional.empty());

			AssetReportResponse response =
					assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.point().balance()).isZero();
		}
	}

	@Nested
	@DisplayName("예적금")
	class 예적금 {

		@Test
		@DisplayName("건수는 진행 중과 수령 대기를 합친 값이다 — 금액과 같은 집합을 센다")
		void 수령_대기도_건수에_넣는다() {
			givenSavings(15_000, 1, 2);

			AssetReportResponse response =
					assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.savings().activeCount()).isEqualTo(3);
		}

		@Test
		@DisplayName("수령 대기만 있어도 건수가 0 이 되지 않는다")
		void 수령_대기만_있어도_건수가_잡힌다() {
			givenSavings(15_000, 0, 1);

			AssetReportResponse response =
					assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			// "0건인데 15000원" 을 막는 것이 이 테스트의 목적이다.
			assertThat(response.savings().activeCount()).isEqualTo(1);
			assertThat(response.savings().totalAmount()).isEqualTo(15_000);
		}
	}

	@Nested
	@DisplayName("투자")
	class 투자 {

		@Test
		@DisplayName("여러 종목의 평가액과 손익을 합산한다")
		void 여러_종목을_합산한다() {
			givenHoldings(
					holding(1, 10_100, 10_250),
					holding(1, 9_900, 9_800),
					holding(1, 10_200, 10_400));

			AssetReportResponse response =
					assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.investment().valuation()).isEqualTo(30_450L);
			assertThat(response.investment().profitLoss()).isEqualTo(150L - 100L + 200L);
		}

		@Test
		@DisplayName("손실이면 손익이 음수다")
		void 손실은_음수다() {
			givenHoldings(holding(1, 10_000, 8_000));

			AssetReportResponse response =
					assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.investment().profitLoss()).isEqualTo(-2_000L);
			assertThat(response.investment().valuation()).isEqualTo(8_000L);
		}

		@Test
		@DisplayName("수량이 1 이 아니어도 곱해서 계산한다")
		void 수량을_곱한다() {
			givenHoldings(holding(3, 1_000, 1_200));

			AssetReportResponse response =
					assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.investment().valuation()).isEqualTo(3_600L);
			assertThat(response.investment().profitLoss()).isEqualTo(600L);
		}

		@Test
		@DisplayName("보유가 없으면 평가액과 손익이 모두 0 이다")
		void 보유가_없으면_0이다() {
			givenHoldings();

			AssetReportResponse response =
					assetReportService.getAssetReport(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.investment().valuation()).isZero();
			assertThat(response.investment().profitLoss()).isZero();
		}
	}

	private void givenPoint(int balance) {
		given(pointWalletRepository.findByChildId(CHILD_ID))
				.willReturn(Optional.of(PointWallet.openWithBalance(CHILD_ID, balance)));
	}

	private void givenSavings(int totalAmount, int activeCount, int maturedCount) {
		given(savingsService.getSummary(any(), any(), any()))
				.willReturn(new SavingsSummaryResponse(activeCount, maturedCount,
						totalAmount, 0, totalAmount, null));
	}

	private void givenHoldings(InvestmentHoldingSummary... holdings) {
		given(investmentProductService.getHoldings(any(), any()))
				.willReturn(new InvestmentHoldingsResponse(List.of(holdings)));
	}

	private InvestmentHoldingSummary holding(int quantity, int buyPrice, int currentPrice) {
		return new InvestmentHoldingSummary(1, "시원아이스", quantity, buyPrice, currentPrice);
	}
}
