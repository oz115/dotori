package com.bana.dotori.auth.service;

import com.bana.dotori.auth.dto.EmailVerificationPurpose;

public interface EmailVerificationService {

	/**
	 * 입력한 이메일로 6자리 인증 코드를 발송한다 (AUTH-07).
	 *
	 * <p>용도가 {@link EmailVerificationPurpose#SIGNUP} 이면 이미 가입된 이메일에
	 * DUPLICATE_EMAIL 을 던지고 코드를 보내지 않는다 — 회원가입 화면이 마지막 가입하기
	 * 단계가 아니라 이메일 입력 시점에 중복을 알 수 있게 하기 위해서다.
	 *
	 * <p>그 외 용도(및 미지정)는 계정 존재 여부를 노출하지 않기 위해 가입 여부와 무관하게
	 * 동일하게 동작한다 — 아이디 찾기·비밀번호 재설정은 가입된 이메일이어야 코드가 의미 있다.
	 */
	void sendCode(String email, EmailVerificationPurpose purpose);

	/**
	 * 6자리 코드를 검증하고 후속 요청용 verificationToken 을 발급한다 (AUTH-07).
	 *
	 * @return 발급된 verificationToken
	 */
	String confirmCode(String email, String code);
}
