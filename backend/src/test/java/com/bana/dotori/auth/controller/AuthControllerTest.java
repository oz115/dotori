package com.bana.dotori.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.auth.dto.LoginRequest;
import com.bana.dotori.auth.dto.LoginResponse;
import com.bana.dotori.auth.dto.LogoutRequest;
import com.bana.dotori.auth.dto.TokenRefreshRequest;
import com.bana.dotori.auth.dto.TokenRefreshResponse;
import com.bana.dotori.auth.service.AuthService;
import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.user.entity.UserRole;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private static final String LOGIN_URL = "/api/v1/auth/login";
	private static final String REFRESH_URL = "/api/v1/auth/refresh";
	private static final String LOGOUT_URL = "/api/v1/auth/logout";

	private static final String LOGIN_BODY = """
		{
			"loginId": "dotori123",
			"password": "P@ssw0rd!"
		}
		""";

	private static final String REFRESH_BODY = """
		{
			"refreshToken": "old-refresh-token"
		}
		""";

	@Test
	@DisplayName("로그인 성공 시 200과 토큰·유저 정보를 반환한다")
	void login_success() throws Exception {
		LoginResponse response = new LoginResponse("access-token", "refresh-token", "Bearer", 3600,
			new LoginResponse.UserSummary(1, "김도토", UserRole.PARENT));
		given(authService.login(any(LoginRequest.class))).willReturn(response);

		mockMvc.perform(post(LOGIN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(LOGIN_BODY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("access-token"))
			.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(3600))
			.andExpect(jsonPath("$.user.userId").value(1))
			.andExpect(jsonPath("$.user.name").value("김도토"))
			.andExpect(jsonPath("$.user.role").value("PARENT"));
	}

	@Test
	@DisplayName("아이디·비밀번호가 일치하지 않으면 401과 INVALID_CREDENTIALS 에러를 반환한다")
	void login_invalidCredentials() throws Exception {
		given(authService.login(any(LoginRequest.class)))
			.willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		mockMvc.perform(post(LOGIN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(LOGIN_BODY))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	@DisplayName("필수 값이 누락되면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void login_invalidRequest() throws Exception {
		mockMvc.perform(post(LOGIN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\": \"\", \"password\": \"P@ssw0rd!\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(authService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("재발급 성공 시 200과 새 토큰을 반환한다")
	void refresh_success() throws Exception {
		given(authService.refresh(any(TokenRefreshRequest.class)))
			.willReturn(new TokenRefreshResponse("new-access", "new-refresh", "Bearer", 3600));

		mockMvc.perform(post(REFRESH_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(REFRESH_BODY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("new-access"))
			.andExpect(jsonPath("$.refreshToken").value("new-refresh"));
	}

	@Test
	@DisplayName("유효하지 않은 Refresh Token이면 401과 INVALID_REFRESH_TOKEN 에러를 반환한다")
	void refresh_invalidToken() throws Exception {
		given(authService.refresh(any(TokenRefreshRequest.class)))
			.willThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

		mockMvc.perform(post(REFRESH_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(REFRESH_BODY))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	@DisplayName("로그아웃 성공 시 204를 반환한다")
	void logout_success() throws Exception {
		given(jwtTokenProvider.getAuthentication("valid-access-token"))
			.willReturn(new UsernamePasswordAuthenticationToken(1, null,
				List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));

		mockMvc.perform(post(LOGOUT_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(REFRESH_BODY))
			.andExpect(status().isNoContent());

		then(authService).should().logout(eq(1), any(LogoutRequest.class));
	}

	@Test
	@DisplayName("Access Token 없이 로그아웃하면 401과 UNAUTHORIZED 에러를 반환한다")
	void logout_withoutAccessToken() throws Exception {
		mockMvc.perform(post(LOGOUT_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(REFRESH_BODY))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(authService).shouldHaveNoInteractions();
	}
}
