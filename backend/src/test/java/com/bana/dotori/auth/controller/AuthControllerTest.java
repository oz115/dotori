package com.bana.dotori.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
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

import com.bana.dotori.auth.dto.EmailVerificationPurpose;
import com.bana.dotori.auth.dto.LoginRequest;
import com.bana.dotori.auth.dto.LoginResponse;
import com.bana.dotori.auth.dto.TokenRefreshResponse;
import com.bana.dotori.auth.service.AuthService;
import com.bana.dotori.auth.service.EmailVerificationService;
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
	private EmailVerificationService emailVerificationService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private static final String LOGIN_URL = "/api/v1/auth/login";
	private static final String REFRESH_URL = "/api/v1/auth/refresh";
	private static final String LOGOUT_URL = "/api/v1/auth/logout";
	private static final String EMAIL_SEND_URL = "/api/v1/auth/email-verification/send";

	private static final String LOGIN_BODY = """
		{
			"loginId": "dotori123",
			"password": "P@ssw0rd!"
		}
		""";

	@Test
	@DisplayName("로그인 성공 시 200과 함께 body에 Access Token과 Refresh Token을 내려준다")
	void login_success() throws Exception {
		LoginResponse response = new LoginResponse("access-token", "refresh-token", "Bearer", 1800,
			new LoginResponse.UserSummary(1, "김도토", UserRole.PARENT));
		given(authService.login(any(LoginRequest.class))).willReturn(response);

		mockMvc.perform(post(LOGIN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(LOGIN_BODY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("access-token"))
			.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(1800))
			.andExpect(jsonPath("$.user.userId").value(1));
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
	@DisplayName("재발급 성공 시 200과 새 Access Token, 새 Refresh Token을 body로 내려준다")
	void refresh_success() throws Exception {
		given(authService.refresh("old-refresh-token")).willReturn(
			new TokenRefreshResponse("new-access", "new-refresh", "Bearer", 1800));

		mockMvc.perform(post(REFRESH_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\": \"old-refresh-token\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("new-access"))
			.andExpect(jsonPath("$.refreshToken").value("new-refresh"));
	}

	@Test
	@DisplayName("Refresh Token 없이 재발급을 요청하면 401과 INVALID_REFRESH_TOKEN 에러를 반환한다")
	void refresh_withoutToken() throws Exception {
		mockMvc.perform(post(REFRESH_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

		then(authService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("body 없이 재발급을 요청해도 401과 INVALID_REFRESH_TOKEN 에러를 반환한다")
	void refresh_withoutBody() throws Exception {
		mockMvc.perform(post(REFRESH_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

		then(authService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("유효하지 않은 Refresh Token이면 401과 INVALID_REFRESH_TOKEN 에러를 반환한다")
	void refresh_invalidToken() throws Exception {
		given(authService.refresh("forged-token"))
			.willThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

		mockMvc.perform(post(REFRESH_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\": \"forged-token\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	@DisplayName("이메일 인증 코드 발송: 회원가입 용도로 발송 성공 시 204를 반환한다")
	void sendEmailVerification_signup() throws Exception {
		mockMvc.perform(post(EMAIL_SEND_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"dotori@example.com\", \"purpose\": \"SIGNUP\"}"))
			.andExpect(status().isNoContent());

		then(emailVerificationService).should()
			.sendCode("dotori@example.com", EmailVerificationPurpose.SIGNUP);
	}

	@Test
	@DisplayName("이메일 인증 코드 발송: 회원가입 용도에서 이미 가입된 이메일이면 409와 DUPLICATE_EMAIL 에러를 반환한다")
	void sendEmailVerification_duplicateEmail() throws Exception {
		willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL))
			.given(emailVerificationService).sendCode("dotori@example.com", EmailVerificationPurpose.SIGNUP);

		mockMvc.perform(post(EMAIL_SEND_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"dotori@example.com\", \"purpose\": \"SIGNUP\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
	}

	@Test
	@DisplayName("이메일 인증 코드 발송: 용도 없이 호출하면 기존 동작대로 발송된다 (찾기·재설정 호환)")
	void sendEmailVerification_withoutPurpose() throws Exception {
		mockMvc.perform(post(EMAIL_SEND_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"dotori@example.com\"}"))
			.andExpect(status().isNoContent());

		then(emailVerificationService).should().sendCode("dotori@example.com", null);
	}

	@Test
	@DisplayName("이메일 인증 코드 발송: 허용되지 않은 용도 값이면 400과 INVALID_REQUEST 에러를 반환한다")
	void sendEmailVerification_invalidPurpose() throws Exception {
		mockMvc.perform(post(EMAIL_SEND_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"dotori@example.com\", \"purpose\": \"LOGIN\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(emailVerificationService).shouldHaveNoInteractions();
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
				.content("{\"refreshToken\": \"refresh-token\"}"))
			.andExpect(status().isNoContent());

		then(authService).should().logout(eq(1), eq("refresh-token"));
	}

	@Test
	@DisplayName("Refresh Token 없이 로그아웃하면 401과 UNAUTHORIZED 에러를 반환한다")
	void logout_withoutToken() throws Exception {
		given(jwtTokenProvider.getAuthentication("valid-access-token"))
			.willReturn(new UsernamePasswordAuthenticationToken(1, null,
				List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));

		mockMvc.perform(post(LOGOUT_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(authService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("Access Token 없이 로그아웃하면 401과 UNAUTHORIZED 에러를 반환한다")
	void logout_withoutAccessToken() throws Exception {
		mockMvc.perform(post(LOGOUT_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\": \"refresh-token\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(authService).shouldHaveNoInteractions();
	}
}
