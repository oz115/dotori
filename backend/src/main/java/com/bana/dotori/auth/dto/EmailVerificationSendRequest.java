package com.bana.dotori.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationSendRequest(
	@NotBlank @Email String email,
	/* 발송 용도. SIGNUP 이면 가입된 이메일에 409 를 주고 코드를 보내지 않는다. 미지정 시 기존 동작 */
	EmailVerificationPurpose purpose
) {
}
