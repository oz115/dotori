package com.bana.dotori.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.http.MediaType;
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
import com.bana.dotori.user.dto.ConnectedChildResponse;
import com.bana.dotori.user.dto.ConnectedChildrenResponse;
import com.bana.dotori.user.dto.PasswordChangeRequest;
import com.bana.dotori.user.dto.UserInfoResponse;
import com.bana.dotori.user.dto.UserWithdrawRequest;
import com.bana.dotori.user.dto.UserUpdateRequest;
import com.bana.dotori.user.dto.UserUpdateResponse;
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
	private static final String PASSWORD_URL = "/api/v1/users/{userId}/password";
	private static final String CHILDREN_URL = "/api/v1/users/{userId}/children";

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

	@Test
	@DisplayName("이름 수정에 성공하면 200 과 명세 형식의 응답을 돌려준다")
	void updateUserInfo_success() throws Exception {
		given(userService.updateUserInfo(eq(1), eq(1), any(UserUpdateRequest.class)))
			.willReturn(new UserUpdateResponse(1, "김도토리",
				OffsetDateTime.of(2026, 9, 1, 13, 0, 0, 0, ZoneOffset.ofHours(9))));

		mockMvc.perform(patch(USER_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"김도토리\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(1))
			.andExpect(jsonPath("$.name").value("김도토리"))
			.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	@DisplayName("이름이 공백뿐이면 서비스를 호출하지 않고 400 으로 막는다")
	void updateUserInfo_blankName() throws Exception {
		mockMvc.perform(patch(USER_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(userService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("이름이 50자를 넘으면 서비스를 호출하지 않고 400 으로 막는다")
	void updateUserInfo_tooLongName() throws Exception {
		mockMvc.perform(patch(USER_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"" + "가".repeat(51) + "\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(userService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("남의 계정을 수정하면 403 과 FORBIDDEN 에러를 돌려준다")
	void updateUserInfo_forbidden() throws Exception {
		willThrow(new BusinessException(ErrorCode.FORBIDDEN))
			.given(userService).updateUserInfo(eq(1), eq(2), any(UserUpdateRequest.class));

		mockMvc.perform(patch(USER_URL, 2).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"김도토리\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("비밀번호 변경에 성공하면 본문 없이 204 를 돌려준다")
	void changePassword_success() throws Exception {
		mockMvc.perform(patch(PASSWORD_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\": \"P@ssw0rd!\", \"newPassword\": \"NewP@ssw0rd!\"}"))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));
	}

	@Test
	@DisplayName("현재 비밀번호가 틀리면 401 과 PASSWORD_MISMATCH 에러를 돌려준다")
	void changePassword_mismatch() throws Exception {
		willThrow(new BusinessException(ErrorCode.PASSWORD_MISMATCH))
			.given(userService).changePassword(eq(1), eq(1), any(PasswordChangeRequest.class));

		mockMvc.perform(patch(PASSWORD_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\": \"wrong\", \"newPassword\": \"NewP@ssw0rd!\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));
	}

	@Test
	@DisplayName("새 비밀번호가 정책에 어긋나면 400 과 INVALID_PASSWORD_FORMAT 에러를 돌려준다")
	void changePassword_invalidFormat() throws Exception {
		willThrow(new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT))
			.given(userService).changePassword(eq(1), eq(1), any(PasswordChangeRequest.class));

		mockMvc.perform(patch(PASSWORD_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\": \"P@ssw0rd!\", \"newPassword\": \"short\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_PASSWORD_FORMAT"));
	}

	@Test
	@DisplayName("currentPassword 가 없으면 서비스를 호출하지 않고 400 으로 막는다")
	void changePassword_missingCurrentPassword() throws Exception {
		mockMvc.perform(patch(PASSWORD_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"newPassword\": \"NewP@ssw0rd!\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(userService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("남의 비밀번호를 바꾸려 하면 403 과 FORBIDDEN 에러를 돌려준다")
	void changePassword_forbidden() throws Exception {
		willThrow(new BusinessException(ErrorCode.FORBIDDEN))
			.given(userService).changePassword(eq(1), eq(2), any(PasswordChangeRequest.class));

		mockMvc.perform(patch(PASSWORD_URL, 2).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\": \"P@ssw0rd!\", \"newPassword\": \"NewP@ssw0rd!\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("연결된 자녀 조회에 성공하면 200 과 명세 형식의 응답을 돌려준다")
	void getConnectedChildren_success() throws Exception {
		given(userService.getConnectedChildren(1, 1)).willReturn(new ConnectedChildrenResponse(List.of(
			new ConnectedChildResponse(2, "김아이",
				OffsetDateTime.of(2026, 8, 28, 10, 0, 0, 0, ZoneOffset.ofHours(9))))));

		mockMvc.perform(get(CHILDREN_URL, 1).with(loginAs(1, "PARENT")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.children").isArray())
			.andExpect(jsonPath("$.children[0].userId").value(2))
			.andExpect(jsonPath("$.children[0].name").value("김아이"))
			.andExpect(jsonPath("$.children[0].joinedAt").exists())
			// 프로필 이미지는 쓰지 않기로 했으므로 응답에 남아 있으면 안 된다.
			.andExpect(jsonPath("$.children[0].profileImageUrl").doesNotExist());
	}

	@Test
	@DisplayName("연결된 자녀가 없으면 200 과 빈 배열을 돌려준다")
	void getConnectedChildren_emptyList() throws Exception {
		given(userService.getConnectedChildren(1, 1)).willReturn(new ConnectedChildrenResponse(List.of()));

		mockMvc.perform(get(CHILDREN_URL, 1).with(loginAs(1, "PARENT")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.children").isArray())
			.andExpect(jsonPath("$.children").isEmpty());
	}

	@Test
	@DisplayName("부모 권한이 아니면 403 과 FORBIDDEN 에러를 돌려준다")
	void getConnectedChildren_forbidden() throws Exception {
		willThrow(new BusinessException(ErrorCode.FORBIDDEN))
			.given(userService).getConnectedChildren(1, 1);

		mockMvc.perform(get(CHILDREN_URL, 1).with(loginAs(1, "CHILD")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("탈퇴에 성공하면 본문 없이 204 를 돌려준다")
	void withdraw_success() throws Exception {
		mockMvc.perform(delete(USER_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\": \"P@ssw0rd!\"}"))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));
	}

	@Test
	@DisplayName("잔여 자산이 있으면 409 와 HAS_ACTIVE_ASSETS 에러를 돌려준다")
	void withdraw_hasActiveAssets() throws Exception {
		willThrow(new BusinessException(ErrorCode.HAS_ACTIVE_ASSETS))
			.given(userService).withdraw(eq(1), eq(1), any(UserWithdrawRequest.class));

		mockMvc.perform(delete(USER_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\": \"P@ssw0rd!\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("HAS_ACTIVE_ASSETS"));
	}

	@Test
	@DisplayName("비밀번호가 틀리면 401 과 PASSWORD_MISMATCH 에러를 돌려준다")
	void withdraw_passwordMismatch() throws Exception {
		willThrow(new BusinessException(ErrorCode.PASSWORD_MISMATCH))
			.given(userService).withdraw(eq(1), eq(1), any(UserWithdrawRequest.class));

		mockMvc.perform(delete(USER_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\": \"wrong\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));
	}

	@Test
	@DisplayName("비밀번호가 없으면 서비스를 호출하지 않고 400 으로 막는다")
	void withdraw_missingPassword() throws Exception {
		mockMvc.perform(delete(USER_URL, 1).with(loginAs(1, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(userService).shouldHaveNoInteractions();
	}
}
