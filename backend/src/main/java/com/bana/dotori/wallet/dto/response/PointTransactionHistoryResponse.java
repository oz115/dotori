package com.bana.dotori.wallet.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.bana.dotori.wallet.entity.PointTransaction;

/**
 * WLT-06 거래내역 조회 응답.
 *
 * <pre>
 * {
 *   "childId": 2,
 *   "balance": 6000,
 *   "transactions": [ ... ],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 42,
 *   "totalPages": 3,
 *   "hasNext": true
 * }
 * </pre>
 *
 * <p>현재 잔액을 목록과 같이 내보낸다. 화면이 잔액과 목록을 같이 그리는데 API 를 두 번
 * 부르면 그 사이 거래가 끼어들어 "목록 마지막 잔액"과 "표시된 잔액"이 어긋날 수 있다.
 *
 * <p>거래가 없어도 오류가 아니다. 빈 목록과 현재 잔액을 그대로 돌려준다.
 */
public record PointTransactionHistoryResponse(
		Integer childId,
		int balance,
		List<PointTransactionItem> transactions,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext) {

	public static PointTransactionHistoryResponse of(Integer childId, int balance, Page<PointTransaction> found) {
		return new PointTransactionHistoryResponse(
				childId,
				balance,
				found.getContent().stream().map(PointTransactionItem::from).toList(),
				found.getNumber(),
				found.getSize(),
				found.getTotalElements(),
				found.getTotalPages(),
				found.hasNext());
	}

	/**
	 * 지갑이 아직 없는 경우. 조회할 원장도 없으므로 빈 목록과 잔액 0 이다.
	 */
	public static PointTransactionHistoryResponse empty(Integer childId, int page, int size) {
		return new PointTransactionHistoryResponse(childId, 0, List.of(), page, size, 0L, 0, false);
	}
}
