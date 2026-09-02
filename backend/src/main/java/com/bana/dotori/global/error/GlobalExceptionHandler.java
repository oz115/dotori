package com.bana.dotori.global.error;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.findFirst()
			.orElse(ErrorCode.INVALID_REQUEST.getMessage());
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
