package com.bana.dotori.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * MYPAGE-05 회원 탈퇴 요청. 되돌릴 수 없는 동작이라 본인 확인용 비밀번호를 받는다.
 */
public record UserWithdrawRequest(
	@NotBlank String password
) {
}
