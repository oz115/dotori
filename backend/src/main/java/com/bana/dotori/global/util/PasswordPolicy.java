package com.bana.dotori.global.util;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

/**
 * 비밀번호 정책(공백 없이 8~64자)의 단일 정의.
 *
 * <p>회원가입(SignupRequest의 @Size)·비밀번호 변경(UserServiceImpl)·비밀번호 재설정(AuthServiceImpl)이
 * 모두 여기를 참조한다. 정의가 흩어져 있으면 정책을 바꿀 때 한 곳을 놓친 경로가
 * 약한 비밀번호의 우회로가 된다.
 */
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 8;
	public static final int MAX_LENGTH = 64;

	private PasswordPolicy() {
	}

	/**
	 * 정책 위반이면 INVALID_PASSWORD_FORMAT 예외를 던진다.
	 */
	public static void validate(String rawPassword) {
		if (rawPassword == null || rawPassword.isBlank()
			|| rawPassword.length() < MIN_LENGTH
			|| rawPassword.length() > MAX_LENGTH) {

			throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT,
				"비밀번호는 공백 없이 " + MIN_LENGTH + "자 이상 " + MAX_LENGTH + "자 이하여야 합니다");
		}
	}
}
