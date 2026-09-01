package com.bana.dotori.common.exception;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * 비즈니스 예외를 API 명세의 공통 에러 응답으로 변환한다.
 *
 * <p>서비스 계층은 HTTP 를 전혀 모른 채 도메인 예외만 던지고,
 * 상태 코드 매핑은 전부 여기서 담당한다.
 * 덕분에 컨트롤러 테스트는 "매핑이 맞는가"만, 서비스 테스트는 "예외가 터지는가"만 검증하면 된다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("business rule violated: code={}, message={}", errorCode.getCode(), e.getMessage());
		return ResponseEntity.status(errorCode.getHttpStatus())
				.body(ErrorResponse.of(errorCode, e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));
		log.warn("request validation failed: {}", message);
		return ResponseEntity.status(CommonErrorCode.INVALID_REQUEST.getHttpStatus())
				.body(ErrorResponse.of(CommonErrorCode.INVALID_REQUEST, message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
		log.error("unexpected error", e);
		return ResponseEntity.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
				.body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
	}
}
