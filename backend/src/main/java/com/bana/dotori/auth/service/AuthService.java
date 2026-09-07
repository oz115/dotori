package com.bana.dotori.auth.service;

import com.bana.dotori.auth.dto.FindIdRequest;
import com.bana.dotori.auth.dto.FindIdResponse;
import com.bana.dotori.auth.dto.LoginRequest;
import com.bana.dotori.auth.dto.LoginResult;
import com.bana.dotori.auth.dto.TokenRefreshResult;

public interface AuthService {

	LoginResult login(LoginRequest request);

	TokenRefreshResult refresh(String refreshToken);

	void logout(Integer userId, String refreshToken);

	/**
	 * 해당 사용자의 살아 있는 Refresh Token 을 모두 폐기한다.
	 *
	 * <p>비밀번호 변경(MYPAGE-04)처럼 기존 세션을 전부 끊어야 할 때 쓴다.
	 */
	void revokeAllRefreshTokens(Integer userId);

	/**
	 * AUTH-05 아이디 찾기. 이름+이메일이 일치하는 회원의 마스킹된 로그인 아이디를 안내한다.
	 */
	FindIdResponse findLoginId(FindIdRequest request);
}
