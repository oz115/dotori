package com.bana.dotori.user.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.user.dto.UserInfoResponse;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.service.UserService;

/**
 * 컨트롤러 계층 - HTTP 경계만 검증한다.
 *
 * <p>서비스를 가짜로 대체했으므로 본인 확인이나 조회는 실제로 일어나지 않는다.
 * 여기가 실패하면 원인은 요청 매핑, 응답 직렬화, 예외 → 상태코드 매핑 중 하나다.
 * 본인 확인 규칙 자체는 {@code UserServiceImplTest} 가 검증한다.
 */
@WebMvcTest(MyPageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
@DisplayName("MYPAGE-01 회원 정보 조회 API")
class MyPageControllerTest {

	private static final String USER_URL = "/api/v1/users/{userId}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("본인 조회에 성공하면 200 과 명세 형식의 응답을 돌려준다")
	void getUserInfo_success() throws Exception {
		given(userService.getUserInfo(1, 1)).willReturn(userInfo());

		mockMvc.perform(get(USER_URL, 1).with(loginAs(1, "PARENT")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(1))
			.andExpect(jsonPath("$.loginId").value("dotori123"))
			.andExpect(jsonPath("$.name").value("김도토"))
			.andExpect(jsonPath("$.email").value("dotori@example.com"))
			.andExpect(jsonPath("$.role").value("PARENT"))
			.andExpect(jsonPath("$.emailVerified").value(true))
			.andExpect(jsonPath("$.createdAt").exists())
			// 프로필 이미지는 쓰지 않기로 했으므로 응답에 남아 있으면 안 된다.
			.andExpect(jsonPath("$.profileImageUrl").doesNotExist());
	}

	@Test
	@DisplayName("남의 계정을 조회하면 403 과 FORBIDDEN 에러를 돌려준다")
	void getUserInfo_forbidden() throws Exception {
		willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(userService).getUserInfo(1, 2);

		mockMvc.perform(get(USER_URL, 2).with(loginAs(1, "PARENT")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"))
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	@DisplayName("회원이 없으면 404 와 NOT_FOUND 에러를 돌려준다")
	void getUserInfo_notFound() throws Exception {
		willThrow(new BusinessException(ErrorCode.NOT_FOUND)).given(userService).getUserInfo(1, 1);

		mockMvc.perform(get(USER_URL, 1).with(loginAs(1, "PARENT")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	@DisplayName("인증 없이 호출하면 401 과 UNAUTHORIZED 에러를 돌려준다")
	void getUserInfo_unauthorized() throws Exception {
		mockMvc.perform(get(USER_URL, 1))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private UserInfoResponse userInfo() {
		return new UserInfoResponse(1, "dotori123", "김도토", "dotori@example.com", UserRole.PARENT, true,
			OffsetDateTime.of(2026, 9, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
	}

	private static RequestPostProcessor loginAs(Integer userId, String role) {
		return authentication(new UsernamePasswordAuthenticationToken(userId, null,
			List.of(new SimpleGrantedAuthority("ROLE_" + role))));
	}
}
