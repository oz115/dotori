package com.bana.dotori.wallet.dto.response;

/**
 * WLT-06 거래내역 조회 응답.
 *
 * <pre>
 * {
 *   "childId": 2,
 *   "balance": 6000,
 *   "page": {
 *     "items": [ ... ],
 *     "nextCursor": "MjAyNi0wOS0wMlQxNTowMDowMCs...",
 *     "hasNext": true
 *   }
 * }
 * </pre>
 *
 * <p>목록을 {@code page} 안에 넣어 {@link CursorPage} 를 그대로 노출한다.
 * 주식·예적금 목록도 같은 모양을 쓰면 클라이언트는 무한 스크롤 처리를 한 번만 만들면 된다.
 * 목록을 최상위로 펼치면 화면마다 "items 가 어디 있는지"가 달라진다.
 *
 * <p>현재 잔액을 목록과 같이 내보낸다. 화면이 잔액과 목록을 같이 그리는데 API 를 두 번
 * 부르면 그 사이 거래가 끼어들어 "목록 마지막 잔액"과 "표시된 잔액"이 어긋날 수 있다.
 *
 * <p>거래가 없어도 오류가 아니다. 빈 목록과 현재 잔액을 그대로 돌려준다.
 */
public record PointTransactionHistoryResponse(
		Integer childId,
		int balance,
		CursorPage<PointTransactionItem> page) {

	public static PointTransactionHistoryResponse of(Integer childId, int balance,
			CursorPage<PointTransactionItem> page) {
		return new PointTransactionHistoryResponse(childId, balance, page);
	}

	/**
	 * 지갑이 아직 없는 경우. 조회할 원장도 없으므로 빈 목록과 잔액 0 이다.
	 */
	public static PointTransactionHistoryResponse empty(Integer childId) {
		return new PointTransactionHistoryResponse(childId, 0, CursorPage.empty());
	}
}
