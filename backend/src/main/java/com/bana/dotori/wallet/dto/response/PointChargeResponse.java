package com.bana.dotori.wallet.dto.response;

import java.time.OffsetDateTime;

import com.bana.dotori.wallet.domain.PointTransactionReason;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * WLT-01 충전 응답. API 명세의 형식을 그대로 따른다.
 *
 * <pre>
 * {
 *   "pointTransactionId": 100,
 *   "transactionType": "CHARGE",
 *   "amount": 10000,
 *   "balanceAfter": 15000,
 *   "createdAt": "2026-08-28T12:00:00+09:00"
 * }
 * </pre>
 *
 * <p>명세의 {@code transactionType} 이 "CHARGE" 인 점에 주의한다.
 * DB 의 transaction_type(DEPOSIT/WITHDRAWAL)이 아니라 거래 사유를 노출하는 필드다.
 */
public record PointChargeResponse(
		Integer pointTransactionId,
		PointTransactionReason transactionType,
		int amount,
		int balanceAfter,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
		OffsetDateTime createdAt) {

	public static PointChargeResponse from(PointChangeResult result) {
		return new PointChargeResponse(
				result.pointTransactionId(),
				result.reason(),
				result.amount(),
				result.balanceAfter(),
				result.createdAt());
	}
}
