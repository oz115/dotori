package com.bana.dotori.global.error;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;

/**
 * 비즈니스 예외를 API 명세의 공통 에러 응답으로 변환한다.
 *
 * <p>서비스 계층은 HTTP 를 전혀 모른 채 도메인 예외만 던지고, 상태 코드 매핑은 여기서 담당한다.
 * 덕분에 컨트롤러 테스트는 "매핑이 맞는가"만, 서비스 테스트는 "예외가 터지는가"만 검증하면 된다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		// 사용자에게는 기본 메시지만 나가고, 원인을 좁힐 수 있는 상세는 로그에만 남긴다.
		log.warn("business rule violated: code={}, detail={}", errorCode.name(), e.getMessage());
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.findFirst()
			.orElse(ErrorCode.INVALID_REQUEST.getMessage());
		log.warn("request validation failed: {}", message);
		return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
			.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
		return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
			.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
		return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
			.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("Unhandled exception", e);
		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
