package com.bana.dotori.global.error;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

	/**
	 * 업로드 용량 초과 (CHORE-13).
	 *
	 * <p>스프링이 {@code spring.servlet.multipart.max-file-size} 를 넘는 요청을 컨트롤러에
	 * 닿기 전에 끊는다. 이 핸들러가 없으면 아래 {@code Exception} 으로 떨어져 500 이 나가는데,
	 * 용량 초과는 클라이언트가 고칠 수 있는 문제라 400 이어야 한다.
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
		return ResponseEntity.status(ErrorCode.INVALID_FILE.getStatus())
			.body(ErrorResponse.of(ErrorCode.INVALID_FILE));
	}

	/**
	 * 파일 파트가 없거나 이름이 다른 요청 (CHORE-13).
	 *
	 * <p>{@code @RequestPart("photo")} 가 찾는 파트가 없으면 스프링이 이 예외를 던진다.
	 * 클라이언트가 파트 이름을 {@code file} 로 보내는 흔한 실수인데, 핸들러가 없으면 아래
	 * {@code Exception} 으로 떨어져 500 이 나가고 프론트는 서버 장애로 오해한다.
	 */
	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(MissingServletRequestPartException e) {
		log.warn("multipart part missing: {}", e.getRequestPartName());
		return ResponseEntity.status(ErrorCode.INVALID_FILE.getStatus())
			.body(ErrorResponse.of(ErrorCode.INVALID_FILE));
	}

	/**
	 * multipart 본문 자체가 깨진 요청.
	 *
	 * <p>경계 문자열이 어긋나거나 전송이 중간에 끊긴 경우다. 위 두 핸들러가 더 구체적이라
	 * 용량 초과·파트 누락은 그쪽이 먼저 잡고, 여기는 남는 경우를 받는다.
	 */
	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<ErrorResponse> handleMultipart(MultipartException e) {
		log.warn("malformed multipart request: {}", e.getMessage());
		return ResponseEntity.status(ErrorCode.INVALID_FILE.getStatus())
			.body(ErrorResponse.of(ErrorCode.INVALID_FILE));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("Unhandled exception", e);
		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
