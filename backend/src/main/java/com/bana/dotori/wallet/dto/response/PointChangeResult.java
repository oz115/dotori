package com.bana.dotori.wallet.dto.response;

import java.time.OffsetDateTime;

import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointTransactionType;

/**
 * 포인트 변경 결과.
 *
 * @param replayed 이번 호출로 실제 반영된 것이 아니라, 멱등키로 찾아낸 이전 거래를 그대로 돌려준 경우 true.
 *                 호출자가 "중복 요청이었다"를 알 수 있어야 알림 재발송 같은 부작용을 건너뛸 수 있다.
 */
public record PointChangeResult(
		Integer pointTransactionId,
		Integer walletId,
		PointTransactionType transactionType,
		PointTransactionReason reason,
		int amount,
		int balanceAfter,
		OffsetDateTime createdAt,
		boolean replayed) {

	public static PointChangeResult applied(PointTransaction transaction) {
		return from(transaction, false);
	}

	public static PointChangeResult replayed(PointTransaction transaction) {
		return from(transaction, true);
	}

	private static PointChangeResult from(PointTransaction transaction, boolean replayed) {
		return new PointChangeResult(
				transaction.getPointTransactionId(),
				transaction.getWalletId(),
				transaction.getTransactionType(),
				transaction.getReason(),
				transaction.getAmount(),
				transaction.getBalanceAfter(),
				transaction.getCreatedAt(),
				replayed);
	}
}
