package com.bana.dotori.wallet.dto.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;

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

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static PointTransactionResponse from(PointChangeResult result) {
		return new PointTransactionResponse(
				result.pointTransactionId(),
				result.reason(),
				result.amount(),
				result.balanceAfter(),
				// 항상 KST 오프셋으로 맞춘다.
				// 방금 만든 거래는 +09:00 이지만, 멱등키로 DB 에서 다시 읽어온 거래는
				// timestamptz 특성상 드라이버가 UTC(Z) 로 돌려준다. 같은 시각인데도 표기가 갈려
				// 클라이언트가 문자열을 그대로 쓰면 9시간 차이로 보인다.
				result.createdAt().atZoneSameInstant(KST).toOffsetDateTime());
	}
}
