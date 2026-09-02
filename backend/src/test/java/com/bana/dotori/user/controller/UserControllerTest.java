package com.bana.dotori.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.service.UserService;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private static final String SIGNUP_URL = "/api/v1/auth/signup";

	private static final String VALID_BODY = """
		{
			"loginId": "dotori123",
			"password": "P@ssw0rd!",
			"name": "김도토",
			"email": "dotori@example.com",
			"role": "PARENT"
		}
		""";

	@Test
	@DisplayName("회원가입 성공 시 201과 유저 정보를 반환한다")
	void signup_success() throws Exception {
		SignupResponse response = new SignupResponse(1, "dotori123", "김도토", "dotori@example.com", UserRole.PARENT,
			OffsetDateTime.of(2026, 9, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
		given(userService.signup(any(SignupRequest.class))).willReturn(response);

		mockMvc.perform(post(SIGNUP_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.userId").value(1))
			.andExpect(jsonPath("$.loginId").value("dotori123"))
			.andExpect(jsonPath("$.name").value("김도토"))
			.andExpect(jsonPath("$.email").value("dotori@example.com"))
			.andExpect(jsonPath("$.role").value("PARENT"))
			.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	@DisplayName("아이디가 중복이면 409와 DUPLICATE_LOGIN_ID 에러를 반환한다")
	void signup_duplicateLoginId() throws Exception {
		given(userService.signup(any(SignupRequest.class)))
			.willThrow(new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID));

		mockMvc.perform(post(SIGNUP_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"))
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	@DisplayName("이메일이 중복이면 409와 DUPLICATE_EMAIL 에러를 반환한다")
	void signup_duplicateEmail() throws Exception {
		given(userService.signup(any(SignupRequest.class)))
			.willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

		mockMvc.perform(post(SIGNUP_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
	}

	@Test
	@DisplayName("필수 값이 누락되면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void signup_invalidRequest() throws Exception {
		String invalidBody = """
			{
				"loginId": "",
				"password": "P@ssw0rd!",
				"name": "김도토",
				"email": "dotori@example.com",
				"role": "PARENT"
			}
			""";

		mockMvc.perform(post(SIGNUP_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(userService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("role에 허용되지 않은 값이 오면 400과 INVALID_REQUEST 에러를 반환한다")
	void signup_invalidRole() throws Exception {
		String invalidRoleBody = """
			{
				"loginId": "dotori123",
				"password": "P@ssw0rd!",
				"name": "김도토",
				"email": "dotori@example.com",
				"role": "ADMIN"
			}
			""";

		mockMvc.perform(post(SIGNUP_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidRoleBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(userService).shouldHaveNoInteractions();
	}
}
