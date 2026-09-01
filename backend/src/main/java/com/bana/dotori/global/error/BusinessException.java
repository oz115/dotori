package com.bana.dotori.global.error;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 나타내는 예외의 최상위 타입.
 *
 * <p>규칙마다 이 클래스를 상속한 별도 예외를 두는 것을 권장한다.
 * 테스트가 {@code assertThatThrownBy(...).isInstanceOf(InsufficientPointException.class)} 처럼
 * 어떤 규칙이 깨졌는지를 타입만으로 지목할 수 있기 때문이다.
 *
 * <p>메시지를 따로 받는 생성자를 둔 이유는 로그 때문이다. 사용자에게는 {@link ErrorCode} 의
 * 기본 메시지가 나가지만, 로그에는 "잔액=1000, 차감 요청=3000"처럼 원인을 좁힐 수 있는
 * 값이 남아야 장애를 추적할 수 있다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final transient ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}
}
