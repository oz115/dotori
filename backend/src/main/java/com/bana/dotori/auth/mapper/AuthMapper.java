package com.bana.dotori.auth.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.auth.dto.LoginResponse;
import com.bana.dotori.auth.dto.TokenRefreshResponse;
import com.bana.dotori.user.entity.User;

@Component
public class AuthMapper {

	private static final String TOKEN_TYPE = "Bearer";

	public LoginResponse toLoginResponse(String accessToken, String refreshToken, long expiresIn, User user) {
		return new LoginResponse(accessToken, refreshToken, TOKEN_TYPE, expiresIn,
			new LoginResponse.UserSummary(user.getId(), user.getName(), user.getRole()));
	}

	public TokenRefreshResponse toTokenRefreshResponse(String accessToken, String refreshToken, long expiresIn) {
		return new TokenRefreshResponse(accessToken, refreshToken, TOKEN_TYPE, expiresIn);
	}
}
