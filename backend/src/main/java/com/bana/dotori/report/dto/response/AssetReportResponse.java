package com.bana.dotori.report.dto.response;

/**
 * RPT-04 자산 현황 조회 응답.
 *
 * <pre>
 * {
 *   "childId": 2,
 *   "totalAssets": 41000,
 *   "point": { "balance": 20000 },
 *   "savings": { "totalAmount": 15000, "activeCount": 1 },
 *   "investment": { "valuation": 6000, "profitLoss": 1000 }
 * }
 * </pre>
 *
 * <p>기간 파라미터가 없는 <b>현재 시점 스냅샷</b>이다. 수입·소비 통계(RPT-02·03)가 원장을
 * 기간으로 집계하는 것과 달리, 여기는 "지금 얼마 가지고 있나"만 본다.
 *
 * <p>명세 v1 에 있던 {@code investment.profitLossRate} 는 내보내지 않는다(팀 결정).
 * 필요해지면 클라이언트가 계산할 수 있다 — {@code valuation - profitLoss} 가 매수 원가
 * 합계이므로 {@code profitLoss / (valuation - profitLoss) * 100} 이다. 서버가 반올림해
 * 내보내면 자릿수를 화면마다 다시 정할 수 없다.
 */
public record AssetReportResponse(
	Integer childId,
	long totalAssets,
	PointAsset point,
	SavingsAsset savings,
	InvestmentAsset investment
) {

	/**
	 * 세 자산을 모아 총액까지 채운다.
	 *
	 * <p>{@code totalAssets} 를 따로 받지 않고 여기서 더한다. 호출부가 넘기게 두면
	 * 세 값과 총액이 어긋난 응답을 만들 수 있다.
	 */
	public static AssetReportResponse of(Integer childId, PointAsset point, SavingsAsset savings,
			InvestmentAsset investment) {

		long totalAssets = (long)point.balance() + savings.totalAmount() + investment.valuation();
		return new AssetReportResponse(childId, totalAssets, point, savings, investment);
	}

	/** 보유 포인트. 지갑이 아직 없으면 0 이다 — 오류가 아니다. */
	public record PointAsset(int balance) {
	}

	/**
	 * 예적금. 수령하거나 해지한 건은 이미 지갑으로 빠져나갔으므로 자산에서 빼고,
	 * <b>진행 중(ACTIVE)과 수령 대기(MATURED)</b> 건만 센다.
	 *
	 * @param totalAmount  원금 + 아직 못 받은 이자. 지금 전부 만기가 온다면 받게 될 금액
	 * @param activeCount  위 금액에 해당하는 가입 건수. 금액과 <b>같은 집합</b>을 세므로
	 *                     수령 대기 건도 포함한다 — 한쪽만 세면 "0건인데 15000원"이 된다
	 */
	public record SavingsAsset(int totalAmount, int activeCount) {
	}

	/**
	 * 투자. 보유가 없으면 둘 다 0 이다.
	 *
	 * @param valuation  보유 종목의 현재가 평가액 합
	 * @param profitLoss 평가 손익 (현재가 - 매수가). 손실이면 음수
	 */
	public record InvestmentAsset(long valuation, long profitLoss) {
	}
}
