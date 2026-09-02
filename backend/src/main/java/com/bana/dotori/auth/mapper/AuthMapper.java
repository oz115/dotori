package com.bana.dotori.auth.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.auth.dto.LoginResponse;
import com.bana.dotori.auth.dto.LoginResult;
import com.bana.dotori.auth.dto.TokenRefreshResponse;
import com.bana.dotori.auth.dto.TokenRefreshResult;
import com.bana.dotori.user.entity.User;

@Component
public class AuthMapper {

	private static final String TOKEN_TYPE = "Bearer";

	public LoginResult toLoginResult(String accessToken, String refreshToken, long expiresIn, User user) {
		LoginResponse response = new LoginResponse(accessToken, TOKEN_TYPE, expiresIn,
			new LoginResponse.UserSummary(user.getId(), user.getName(), user.getRole()));
		return new LoginResult(response, refreshToken);
	}

	public TokenRefreshResult toTokenRefreshResult(String accessToken, String refreshToken, long expiresIn) {
		return new TokenRefreshResult(new TokenRefreshResponse(accessToken, TOKEN_TYPE, expiresIn), refreshToken);
	}
}
