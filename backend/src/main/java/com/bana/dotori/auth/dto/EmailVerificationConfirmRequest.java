package com.bana.dotori.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * code 는 형식(@Pattern)으로 거르지 않는다. 형식이 틀린 코드도 결국 저장된 값과
 * 불일치라 INVALID_CODE 로 떨어지는데, Bean Validation 으로 막으면 INVALID_REQUEST 가
 * 나가 명세의 에러 코드와 갈린다 (가족 초대 코드 입력과 같은 방식).
 */
public record EmailVerificationConfirmRequest(
	@NotBlank @Email String email,
	@NotBlank @Size(max = 100) String code
) {
}
