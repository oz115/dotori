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
 */
public record ErrorResponse(
		String code,
		String message,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
		OffsetDateTime timestamp) {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode, errorCode.getMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.getCode(), message, OffsetDateTime.now(KST));
	}
}
