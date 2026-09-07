package com.bana.dotori.auth.service;

public interface EmailVerificationService {

	/**
	 * 입력한 이메일로 6자리 인증 코드를 발송한다 (AUTH-07).
	 *
	 * <p>계정 존재 여부를 노출하지 않기 위해 가입 여부와 무관하게 동일하게 동작한다.
	 */
	void sendCode(String email);

	/**
	 * 6자리 코드를 검증하고 후속 요청용 verificationToken 을 발급한다 (AUTH-07).
	 *
	 * @return 발급된 verificationToken
	 */
	String confirmCode(String email, String code);
}
