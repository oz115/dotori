package com.bana.dotori.wallet.dto.response;

import java.time.OffsetDateTime;

import com.bana.dotori.wallet.entity.PointTransactionReason;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 포인트 거래 한 건의 응답. 충전(WLT-01)과 지급(WLT-02)이 같은 형식을 쓴다.
 *
 * <pre>
 * {
 *   "pointTransactionId": 101,
 *   "transactionType": "GRANT",
 *   "amount": 5000,
 *   "balanceAfter": 20000,
 *   "createdAt": "2026-08-28T12:00:00+09:00"
 * }
 * </pre>
 *
 * <p>명세의 {@code transactionType} 은 DB 의 transaction_type(DEPOSIT/WITHDRAWAL)이 아니라
 * 거래 사유(CHARGE/GRANT/...)를 노출하는 필드다.
 */
public record PointTransactionResponse(
		Integer pointTransactionId,
		PointTransactionReason transactionType,
		int amount,
		int balanceAfter,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
		OffsetDateTime createdAt) {

	public static PointTransactionResponse from(PointChangeResult result) {
		return new PointTransactionResponse(
				result.pointTransactionId(),
				result.reason(),
				result.amount(),
				result.balanceAfter(),
				result.createdAt());
	}
}
