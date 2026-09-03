package com.bana.dotori.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.global.security.RefreshTokenCookieProvider;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.wallet.support.WalletTestFixture;
import jakarta.servlet.http.Cookie;

/**
 * E2E - 마이페이지 요청 하나가 인증부터 실제 DB 까지 끝까지 도는지 확인한다.
 *
 * <p>계층 테스트(MyPageControllerTest·UserServiceImplTest)가 모두 통과해도 배선이
 * 어긋나 있으면 여기서만 깨진다. 특히 계층 테스트로는 확인할 수 없는 것들을 본다.
 * updatedAt 은 @UpdateTimestamp 가 flush 때 채우고 Refresh Token 폐기는 DB 를
 * 왕복해야 확인되는데, Mockito 에는 하이버네이트가 없어 둘 다 단위 테스트로는 검증되지 않는다.
 *
 * <p>회원은 실제 회원가입 API 로 만든다. 비밀번호가 진짜 bcrypt 해시여야
 * 비밀번호 변경과 로그인을 검증할 수 있기 때문이다.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("마이페이지 API E2E")
class MyPageApiE2ETest {

	private static final String USER_URL = "/api/v1/users/{userId}";
	private static final String PASSWORD_URL = "/api/v1/users/{userId}/password";
	private static final String CHILDREN_URL = "/api/v1/users/{userId}/children";
	private static final String PARENT_PASSWORD = "P@ssw0rd!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer parentId;
	private Integer childId;

	@BeforeEach
	void setUp() throws Exception {
		WalletTestFixture.cleanUp(jdbcTemplate);
		parentId = signup("mypage-parent", PARENT_PASSWORD, "김도토", "PARENT");
		childId = signup("mypage-child", "ChildP@ss1", "김아이", "CHILD");
		WalletTestFixture.insertFamilyWithChild(jdbcTemplate, parentId, childId);
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
	}

	@Nested
	@DisplayName("회원 정보 조회 (MYPAGE-01)")
	class GetUserInfo {

		@Test
		@DisplayName("본인 조회는 명세 형식으로 응답하고 프로필 이미지 필드가 없다")
		void 본인_조회() throws Exception {
			mockMvc.perform(get(USER_URL, parentId).header(HttpHeaders.AUTHORIZATION, bearer(parentId, "PARENT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(parentId))
				.andExpect(jsonPath("$.loginId").value("mypage-parent"))
				.andExpect(jsonPath("$.name").value("김도토"))
				.andExpect(jsonPath("$.role").value("PARENT"))
				.andExpect(jsonPath("$.emailVerified").value(false))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.profileImageUrl").doesNotExist());
		}

		@Test
		@DisplayName("남의 계정을 조회하면 403 이다")
		void 타인_조회는_403() throws Exception {
			mockMvc.perform(get(USER_URL, parentId).header(HttpHeaders.AUTHORIZATION, bearer(childId, "CHILD")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		}

		@Test
		@DisplayName("토큰 없이 호출하면 401 이다")
		void 미인증은_401() throws Exception {
			mockMvc.perform(get(USER_URL, parentId))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		}
	}

	@Nested
	@DisplayName("회원 정보 수정 (MYPAGE-02)")
	class UpdateUserInfo {

		@Test
		@DisplayName("이름을 바꾸면 DB 에 반영되고 updated_at 이 실제로 갱신된다")
		void 이름_수정이_DB까지_반영된다() throws Exception {
			OffsetDateTime before = selectUpdatedAt(parentId);

			mockMvc.perform(patch(USER_URL, parentId)
					.header(HttpHeaders.AUTHORIZATION, bearer(parentId, "PARENT"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\": \"김도토리\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("김도토리"))
				.andExpect(jsonPath("$.updatedAt").exists());

			assertThat(selectName(parentId)).isEqualTo("김도토리");
			// @UpdateTimestamp 는 flush 시점에 채운다. saveAndFlush 를 빼면 여기서만 깨진다.
			assertThat(selectUpdatedAt(parentId)).isAfter(before);
		}

		@Test
		@DisplayName("보내지 않은 필드는 그대로 남는다")
		void 미전달_필드는_유지된다() throws Exception {
			mockMvc.perform(patch(USER_URL, parentId)
					.header(HttpHeaders.AUTHORIZATION, bearer(parentId, "PARENT"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isOk());

			assertThat(selectName(parentId)).isEqualTo("김도토");
		}

		@Test
		@DisplayName("이름이 공백뿐이면 400 이고 DB 도 그대로다")
		void 공백_이름은_400() throws Exception {
			mockMvc.perform(patch(USER_URL, parentId)
					.header(HttpHeaders.AUTHORIZATION, bearer(parentId, "PARENT"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\": \"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

			assertThat(selectName(parentId)).isEqualTo("김도토");
		}
	}

	@Nested
	@DisplayName("비밀번호 변경 (MYPAGE-04)")
	class ChangePassword {

		@Test
		@DisplayName("변경에 성공하면 새 비밀번호로 로그인되고 기존 비밀번호는 막힌다")
		void 변경_후_로그인이_바뀐다() throws Exception {
			changePassword(PARENT_PASSWORD, "NewP@ssw0rd!").andExpect(status().isNoContent());

			login("mypage-parent", "NewP@ssw0rd!").andExpect(status().isOk());
			login("mypage-parent", PARENT_PASSWORD).andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("변경에 성공하면 변경 전에 받은 Refresh Token 이 무효가 된다")
		void 기존_Refresh_Token_이_끊긴다() throws Exception {
			// 폐기 전에는 재발급이 된다는 것부터 확인해야, 뒤의 실패가 폐기 때문임이 분명해진다.
			Cookie beforeChange = loginAndGetRefreshCookie("mypage-parent", PARENT_PASSWORD);
			mockMvc.perform(post("/api/v1/auth/refresh").cookie(beforeChange))
				.andExpect(status().isOk());

			Cookie stillValid = loginAndGetRefreshCookie("mypage-parent", PARENT_PASSWORD);
			changePassword(PARENT_PASSWORD, "NewP@ssw0rd!").andExpect(status().isNoContent());

			mockMvc.perform(post("/api/v1/auth/refresh").cookie(stillValid))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
		}

		@Test
		@DisplayName("현재 비밀번호가 틀리면 401 이고 비밀번호는 그대로다")
		void 현재_비밀번호_불일치는_401() throws Exception {
			changePassword("wrong-password", "NewP@ssw0rd!")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));

			login("mypage-parent", PARENT_PASSWORD).andExpect(status().isOk());
		}

		@Test
		@DisplayName("새 비밀번호가 정책에 어긋나면 400 이고 비밀번호는 그대로다")
		void 정책_위반은_400() throws Exception {
			changePassword(PARENT_PASSWORD, "short")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PASSWORD_FORMAT"));

			login("mypage-parent", PARENT_PASSWORD).andExpect(status().isOk());
		}
	}

	@Nested
	@DisplayName("연결된 자녀 조회 (MYPAGE-06)")
	class GetConnectedChildren {

		@Test
		@DisplayName("부모가 조회하면 같은 가족의 자녀가 나온다")
		void 부모_조회() throws Exception {
			mockMvc.perform(get(CHILDREN_URL, parentId)
					.header(HttpHeaders.AUTHORIZATION, bearer(parentId, "PARENT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.children.length()").value(1))
				.andExpect(jsonPath("$.children[0].userId").value(childId))
				.andExpect(jsonPath("$.children[0].name").value("김아이"))
				.andExpect(jsonPath("$.children[0].joinedAt").exists())
				.andExpect(jsonPath("$.children[0].profileImageUrl").doesNotExist());
		}

		@Test
		@DisplayName("가족에서 빠진 자녀는 목록에서 사라진다")
		void 가족에서_빠지면_안_보인다() throws Exception {
			jdbcTemplate.update("UPDATE family_members SET deleted_at = now() WHERE user_id = ?", childId);

			mockMvc.perform(get(CHILDREN_URL, parentId)
					.header(HttpHeaders.AUTHORIZATION, bearer(parentId, "PARENT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.children").isEmpty());
		}

		@Test
		@DisplayName("자녀 계정이 자기 자녀 목록을 부르면 403 이다")
		void 자녀_계정은_403() throws Exception {
			mockMvc.perform(get(CHILDREN_URL, childId)
					.header(HttpHeaders.AUTHORIZATION, bearer(childId, "CHILD")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		}
	}

	@Nested
	@DisplayName("회원 탈퇴 (MYPAGE-05)")
	class Withdraw {

		@Test
		@DisplayName("탈퇴하면 로그인도 토큰 재발급도 막힌다")
		void 탈퇴하면_계정이_막힌다() throws Exception {
			Cookie refreshCookie = loginAndGetRefreshCookie("mypage-parent", PARENT_PASSWORD);

			withdraw(PARENT_PASSWORD).andExpect(status().isNoContent());

			login("mypage-parent", PARENT_PASSWORD).andExpect(status().isUnauthorized());
			mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
				.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("탈퇴한 자녀는 부모의 연결된 자녀 목록에서 사라진다")
		void 탈퇴한_자녀는_목록에서_사라진다() throws Exception {
			mockMvc.perform(delete(USER_URL, childId)
					.header(HttpHeaders.AUTHORIZATION, bearer(childId, "CHILD"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"password\": \"ChildP@ss1\"}"))
				.andExpect(status().isNoContent());

			// 가족 소속 행은 그대로 남는다. 조회 쿼리가 users.deleted_at 을 보고 걸러 내는지 확인한다.
			mockMvc.perform(get(CHILDREN_URL, parentId)
					.header(HttpHeaders.AUTHORIZATION, bearer(parentId, "PARENT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.children").isEmpty());
		}

		@Test
		@DisplayName("포인트가 남아 있으면 409 로 막고 계정은 살아 있다")
		void 잔여_포인트가_있으면_409() throws Exception {
			jdbcTemplate.update("INSERT INTO point_wallets (user_id, balance) VALUES (?, ?)", parentId, 1_000);

			withdraw(PARENT_PASSWORD)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("HAS_ACTIVE_ASSETS"));

			login("mypage-parent", PARENT_PASSWORD).andExpect(status().isOk());
		}

		@Test
		@DisplayName("잔액이 0 이면 지갑이 있어도 탈퇴된다")
		void 잔액_0_이면_탈퇴된다() throws Exception {
			jdbcTemplate.update("INSERT INTO point_wallets (user_id, balance) VALUES (?, ?)", parentId, 0);

			withdraw(PARENT_PASSWORD).andExpect(status().isNoContent());
		}

		@Test
		@DisplayName("비밀번호가 틀리면 401 이고 계정은 살아 있다")
		void 비밀번호_불일치는_401() throws Exception {
			withdraw("wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));

			login("mypage-parent", PARENT_PASSWORD).andExpect(status().isOk());
		}
	}


	private Integer signup(String loginId, String password, String name, String role) throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"loginId": "%s", "password": "%s", "name": "%s", "email": "%s@dotori.test", "role": "%s"}
					""".formatted(loginId, password, name, loginId, role)))
			.andExpect(status().isCreated());
		return jdbcTemplate.queryForObject(
			"SELECT user_id FROM users WHERE login_id = ?", Integer.class, loginId);
	}

	private ResultActions login(String loginId, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\": \"%s\", \"password\": \"%s\"}".formatted(loginId, password)));
	}

	private Cookie loginAndGetRefreshCookie(String loginId, String password) throws Exception {
		MvcResult result = login(loginId, password).andExpect(status().isOk()).andReturn();
		Cookie cookie = result.getResponse().getCookie(RefreshTokenCookieProvider.COOKIE_NAME);
		assertThat(cookie).isNotNull();
		return cookie;
	}

	private ResultActions withdraw(String password) throws Exception {
		return mockMvc.perform(delete(USER_URL, parentId)
			.header(HttpHeaders.AUTHORIZATION, bearer(parentId, "PARENT"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"password\": \"%s\"}".formatted(password)));
	}

	private ResultActions changePassword(String current, String next) throws Exception {
		return mockMvc.perform(patch(PASSWORD_URL, parentId)
			.header(HttpHeaders.AUTHORIZATION, bearer(parentId, "PARENT"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"currentPassword\": \"%s\", \"newPassword\": \"%s\"}".formatted(current, next)));
	}

	private String selectName(Integer userId) {
		return jdbcTemplate.queryForObject("SELECT name FROM users WHERE user_id = ?", String.class, userId);
	}

	private OffsetDateTime selectUpdatedAt(Integer userId) {
		return jdbcTemplate.queryForObject("SELECT updated_at FROM users WHERE user_id = ?",
			OffsetDateTime.class, userId);
	}

	private String bearer(Integer userId, String role) {
		return "Bearer " + jwtTokenProvider.createAccessToken(userId, UserRole.valueOf(role));
	}
}
