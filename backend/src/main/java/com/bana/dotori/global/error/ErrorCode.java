package com.bana.dotori.global.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
	NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),

	DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다"),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),

	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다"),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다"),

	ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 연동된 계좌입니다"),
	AUTH_SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "1원 인증 요청이 없거나 만료되었습니다"),
	AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "1원 인증코드가 일치하지 않습니다"),
	AUTH_ATTEMPT_EXCEEDED(HttpStatus.BAD_REQUEST, "1원 인증 시도 횟수를 초과했습니다. 다시 요청해 주세요"),
	FINANCE_API_ERROR(HttpStatus.BAD_GATEWAY, "금융망 처리 중 오류가 발생했습니다");

	private final HttpStatus status;
	private final String message;
}
