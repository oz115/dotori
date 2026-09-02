package com.bana.dotori.auth.service;

import com.bana.dotori.auth.dto.LoginRequest;
import com.bana.dotori.auth.dto.LoginResult;
import com.bana.dotori.auth.dto.TokenRefreshResult;

public interface AuthService {

	LoginResult login(LoginRequest request);

	TokenRefreshResult refresh(String refreshToken);

	void logout(Integer userId, String refreshToken);
}
