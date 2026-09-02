package com.bana.dotori.global.error;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * API 명세의 공통 에러 응답 형식.
 *
 * <pre>
 * {
 *   "code": "INSUFFICIENT_POINT",
 *   "message": "보유 포인트가 부족합니다",
 *   "timestamp": "2026-08-28T12:00:00+09:00"
 * }
 * </pre>
 *
 * <p>{@code code} 는 {@link ErrorCode} 상수 이름을 그대로 쓴다.
 * {@code timestamp} 는 명세대로 KST 오프셋을 붙여 내보낸다. 서버 타임존에 따라
 * 오프셋이 달라지면 클라이언트가 시간을 잘못 해석할 수 있어 고정한다.
 */
public record ErrorResponse(
	String code,
	String message,
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
	OffsetDateTime timestamp
) {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage(), OffsetDateTime.now(KST));
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message, OffsetDateTime.now(KST));
	}
}
