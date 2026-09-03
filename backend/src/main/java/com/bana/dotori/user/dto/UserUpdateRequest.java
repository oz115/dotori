package com.bana.dotori.user.dto;

import jakarta.validation.constraints.Size;

/**
 * MYPAGE-02 회원 정보 수정 요청.
 *
 * <p>부분 수정이라 필드를 아예 안 보내는 건 "그대로 둔다"는 뜻이므로 허용한다.
 * 다만 보냈는데 공백뿐이면 거부해야 한다. 통과시키면 이름이 공백으로 지워진다.
 * 그래서 앞뒤 공백을 먼저 털어내고, 그 결과가 빈 문자열이면 {@code @Size} 가 걸러낸다.
 */
public record UserUpdateRequest(
	@Size(min = 1, max = 50)
	String name
) {

	public UserUpdateRequest {
		if (name != null) {
			name = name.trim();
		}
	}
}
