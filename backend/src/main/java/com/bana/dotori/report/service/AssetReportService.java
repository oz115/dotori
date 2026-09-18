package com.bana.dotori.report.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.investment.dto.InvestmentHoldingSummary;
import com.bana.dotori.investment.service.InvestmentProductService;
import com.bana.dotori.report.dto.response.AssetReportResponse;
import com.bana.dotori.report.dto.response.AssetReportResponse.InvestmentAsset;
import com.bana.dotori.report.dto.response.AssetReportResponse.PointAsset;
import com.bana.dotori.report.dto.response.AssetReportResponse.SavingsAsset;
import com.bana.dotori.savings.dto.response.SavingsSummaryResponse;
import com.bana.dotori.savings.service.SavingsService;
import com.bana.dotori.wallet.repository.PointWalletRepository;

import lombok.RequiredArgsConstructor;

/**
 * 자산 현황 (RPT-04). 포인트·예적금·투자를 한 번에 모아 지금 시점의 자산을 보여 준다.
 *
 * <p>{@code PointReportService} 와 나눠 둔 이유는 하는 일이 다르기 때문이다. 그쪽은 원장
 * 한 테이블을 기간으로 집계하고, 여기는 세 도메인의 <b>현재 상태</b>를 모은다. 기간
 * 파라미터도 없다.
 *
 * <p>세 도메인의 기존 서비스를 그대로 쓴다. 특히 투자 평가액은 직접 계산하지 않는다 —
 * 현재가가 단순히 상품의 {@code currentPrice} 가 아니라 <b>가족별 가격 이력에서 찾은 최신
 * 가격</b>이고, 없으면 상품 가격으로 폴백하는 규칙이 있다. 그 규칙을 여기 옮겨 적으면
 * 투자 탭과 리포트가 서로 다른 평가액을 보여주게 된다.
 */
@Service
@RequiredArgsConstructor
public class AssetReportService {

	private final PointWalletRepository pointWalletRepository;
	private final SavingsService savingsService;
	private final InvestmentProductService investmentProductService;
	private final ReportAccessValidator reportAccessValidator;

	/**
	 * RPT-04 자산 현황 조회.
	 *
	 * <p><b>권한 검증을 반드시 먼저 한다.</b> 아래에서 부르는 예적금·투자 서비스도 각자
	 * 권한을 다시 보는데, 그쪽이 던지는 코드는 {@code FAMILY_NOT_FOUND}(404)·
	 * {@code MEMBER_NOT_FOUND}(404)·{@code FORBIDDEN}(403) 이라 RPT 명세의
	 * {@code NOT_FAMILY_CHILD}(403) 와 다르다. 이 검증이 먼저 걸러 주는 덕에 실제로는
	 * 통과한 요청만 내려가지만, 순서가 바뀌면 다른 도메인의 에러 코드가 리포트 응답으로
	 * 새어 나간다.
	 *
	 * @param familyId    경로의 가족
	 * @param childId     조회 대상 자녀
	 * @param loginUserId 호출자. 그 가족의 부모이거나 자녀 본인이어야 한다
	 */
	@Transactional(readOnly = true)
	public AssetReportResponse getAssetReport(Integer familyId, Integer childId, Integer loginUserId) {
		reportAccessValidator.validate(familyId, childId, loginUserId);

		return AssetReportResponse.of(childId,
				readPoint(childId),
				readSavings(familyId, childId, loginUserId),
				readInvestment(childId, loginUserId));
	}

	/**
	 * 보유 포인트.
	 *
	 * <p>{@code PointWalletQueryService.getBalance} 를 쓰지 않고 리포지토리를 직접 읽는다.
	 * 그 메서드는 권한을 다시 보면서 {@code WALLET_ACCESS_DENIED} 를 던지는데, 위에서 이미
	 * 판정을 끝냈고 코드도 명세와 다르다.
	 *
	 * <p>지갑이 없으면 0 이다. 아직 포인트를 한 번도 받지 않은 아이라는 뜻이고 오류가 아니다.
	 */
	private PointAsset readPoint(Integer childId) {
		int balance = pointWalletRepository.findByChildId(childId)
				.map(wallet -> wallet.getBalance())
				.orElse(0);
		return new PointAsset(balance);
	}

	/**
	 * 예적금 적립액과 건수.
	 *
	 * <p>집계 자체는 예적금 쪽 요약(SAV 요약)을 그대로 쓴다. 그쪽이 이미 진행 중·수령 대기
	 * 건만 모아 원금과 이자를 더해 두었다.
	 *
	 * <p>건수는 {@code activeCount + maturedCount} 다. 요약의 {@code activeCount} 만 쓰면
	 * 금액은 수령 대기 건을 포함하는데 건수는 빠져서 "0건인데 15000원"이 나온다.
	 */
	private SavingsAsset readSavings(Integer familyId, Integer childId, Integer loginUserId) {
		SavingsSummaryResponse summary = savingsService.getSummary(loginUserId, familyId, childId);
		return new SavingsAsset(summary.totalAmount(), summary.activeCount() + summary.maturedCount());
	}

	/**
	 * 투자 평가액과 평가 손익.
	 *
	 * <p>수량을 곱한다. 지금은 종목당 1주 규칙이라 항상 1 이지만, 규칙이 풀렸을 때 조용히
	 * 틀리지 않게 곱셈으로 둔다.
	 *
	 * <p>합계를 {@code long} 으로 받는다. 종목 수 × 가격이라 {@code int} 상한을 넘길 만한
	 * 규모는 아니지만, 자산 총액까지 더해지는 값이라 넘침을 걱정할 필요가 없게 해 둔다.
	 */
	private InvestmentAsset readInvestment(Integer childId, Integer loginUserId) {
		List<InvestmentHoldingSummary> holdings =
				investmentProductService.getHoldings(loginUserId, childId).holdings();

		long valuation = 0;
		long profitLoss = 0;
		for (InvestmentHoldingSummary holding : holdings) {
			long quantity = holding.quantity();
			valuation += (long)holding.currentPrice() * quantity;
			profitLoss += ((long)holding.currentPrice() - holding.buyPrice()) * quantity;
		}

		return new InvestmentAsset(valuation, profitLoss);
	}
}
