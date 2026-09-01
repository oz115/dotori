package com.bana.dotori.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인별 에러 코드가 구현하는 계약.
 *
 * <p>테스트에서 "어느 로직이 깨졌는지"를 코드 하나로 판별할 수 있도록,
 * 비즈니스 규칙 위반마다 서로 다른 코드를 부여한다.
 */
public interface ErrorCode {

	/**
	 * API 명세의 공통 에러 응답에 실리는 코드 문자열. (예: INSUFFICIENT_POINT)
	 */
	String getCode();

	/**
	 * 사용자에게 노출되는 기본 메시지.
	 */
	String getMessage();

	/**
	 * 이 에러가 매핑되는 HTTP 상태.
	 */
	HttpStatus getHttpStatus();
}
