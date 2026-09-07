package com.bana.dotori.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * AUTH-06 비밀번호 재설정 요청.
 *
 * <p>newPassword 에는 제약을 걸지 않는다. 명세가 비밀번호 정책 위반을
 * INVALID_PASSWORD_FORMAT 으로 규정하는데, Bean Validation 으로 막으면
 * INVALID_REQUEST 가 나가 코드가 갈린다. 정책 검사는 서비스가 맡는다
 * ({@code PasswordChangeRequest}와 같은 이유).
 */
public record PasswordResetRequest(
	@NotBlank String loginId,
	@NotBlank @Email String email,
	@NotBlank String verificationToken,
	String newPassword
) {
}
