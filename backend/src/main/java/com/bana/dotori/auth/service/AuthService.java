package com.bana.dotori.auth.service;

import com.bana.dotori.auth.dto.LoginRequest;
import com.bana.dotori.auth.dto.LoginResponse;
import com.bana.dotori.auth.dto.LogoutRequest;
import com.bana.dotori.auth.dto.TokenRefreshRequest;
import com.bana.dotori.auth.dto.TokenRefreshResponse;

public interface AuthService {

	LoginResponse login(LoginRequest request);

	TokenRefreshResponse refresh(TokenRefreshRequest request);

	void logout(Integer userId, LogoutRequest request);
}
