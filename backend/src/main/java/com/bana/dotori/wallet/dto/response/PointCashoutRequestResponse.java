package com.bana.dotori.wallet.dto.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.bana.dotori.wallet.entity.PointCashoutRequestStatus;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * WLT-07 환전 요청 한 건의 응답.
 *
 * <pre>
 * {
 *   "cashoutRequestId": 12,
 *   "childId": 3,
 *   "parentId": 2,
 *   "pointAmount": 1000,
 *   "status": "PENDING",
 *   "requestedAt": "2026-08-28T12:00:00+09:00",
 *   "processedAt": null
 * }
 * </pre>
 */
public record PointCashoutRequestResponse(
		Integer cashoutRequestId,
		Integer childId,
		Integer parentId,
		int pointAmount,
		PointCashoutRequestStatus status,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
		OffsetDateTime requestedAt,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
		OffsetDateTime processedAt) {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static PointCashoutRequestResponse from(PointCashoutRequestResult result) {
		return new PointCashoutRequestResponse(
				result.cashoutRequestId(),
				result.childId(),
				result.parentId(),
				result.pointAmount(),
				result.status(),
				// PointTransactionResponse 와 같은 이유로 항상 KST 오프셋으로 맞춘다.
				// 방금 만든 요청은 +09:00 이지만, DB 에서 다시 읽어온 값은 timestamptz 특성상
				// UTC(Z) 로 돌아와 클라이언트가 그대로 쓰면 9시간 차이로 보인다.
				toKst(result.requestedAt()),
				toKst(result.processedAt()));
	}

	private static OffsetDateTime toKst(OffsetDateTime value) {
		return value == null ? null : value.atZoneSameInstant(KST).toOffsetDateTime();
	}
}
