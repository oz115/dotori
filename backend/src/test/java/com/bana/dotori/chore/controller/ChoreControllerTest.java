package com.bana.dotori.chore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.service.ChoreService;
import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;

@WebMvcTest(ChoreController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class ChoreControllerTest {

	private static final String CHORES_URL = "/api/v1/chores";
	private static final String TOKEN = "access-token";
	private static final String BEARER = "Bearer " + TOKEN;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChoreService choreService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated(Integer userId) {
		given(jwtTokenProvider.getAuthentication(TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));
	}

	private String body(String childId, String title, String rewardPoints, String repeatType) {
		return """
			{
			  "childId": %s,
			  "title": %s,
			  "description": "저녁 먹고 나서",
			  "rewardPoints": %s,
			  "repeatType": %s,
			  "startDate": "2026-09-01",
			  "endDate": "2026-09-30"
			}
			""".formatted(childId, title, rewardPoints, repeatType);
	}

	private String validBody() {
		return body("2", "\"설거지\"", "500", "\"DAY\"");
	}

	@Test
	@DisplayName("생성 성공 시 201과 생성된 집안일을 반환한다")
	void createChore_success() throws Exception {
		givenAuthenticated(1);
		given(choreService.createChore(eq(1), any(ChoreCreateRequest.class))).willReturn(new ChoreDetailResponse(
			7, 10, 1, 2, "설거지", "저녁 먹고 나서", 500, ChoreRepeatType.DAY,
			LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), true,
			OffsetDateTime.of(2026, 9, 8, 9, 0, 0, 0, ZoneOffset.ofHours(9))));

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBody()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.choreId").value(7))
			.andExpect(jsonPath("$.familyId").value(10))
			.andExpect(jsonPath("$.parentId").value(1))
			.andExpect(jsonPath("$.childId").value(2))
			.andExpect(jsonPath("$.title").value("설거지"))
			.andExpect(jsonPath("$.rewardPoints").value(500))
			.andExpect(jsonPath("$.repeatType").value("DAY"))
			.andExpect(jsonPath("$.startDate").value("2026-09-01"))
			.andExpect(jsonPath("$.endDate").value("2026-09-30"))
			.andExpect(jsonPath("$.isActive").value(true));
	}

	@Test
	@DisplayName("토큰 없이 요청하면 401을 반환하고 서비스는 호출하지 않는다")
	void createChore_unauthorized() throws Exception {
		mockMvc.perform(post(CHORES_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBody()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		verify(choreService, never()).createChore(any(), any());
	}

	@Test
	@DisplayName("아이가 호출하면 403과 FORBIDDEN 에러를 반환한다")
	void createChore_forbidden() throws Exception {
		givenAuthenticated(1);
		given(choreService.createChore(eq(1), any(ChoreCreateRequest.class)))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBody()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("내 가족의 자녀가 아니면 404와 MEMBER_NOT_FOUND 에러를 반환한다")
	void createChore_memberNotFound() throws Exception {
		givenAuthenticated(1);
		given(choreService.createChore(eq(1), any(ChoreCreateRequest.class)))
			.willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBody()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}

	@Test
	@DisplayName("필수 값이 빠지면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출하지 않는다")
	void createChore_invalidRequest() throws Exception {
		givenAuthenticated(1);

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("null", "\"설거지\"", "500", "\"DAY\"")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		verify(choreService, never()).createChore(any(), any());
	}

	@Test
	@DisplayName("보상이 0이면 400을 반환하고 서비스는 호출하지 않는다")
	void createChore_nonPositiveReward() throws Exception {
		givenAuthenticated(1);

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("2", "\"설거지\"", "0", "\"DAY\"")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		verify(choreService, never()).createChore(any(), any());
	}

	@Test
	@DisplayName("없는 반복 주기를 보내면 400을 반환한다")
	void createChore_unknownRepeatType() throws Exception {
		givenAuthenticated(1);

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("2", "\"설거지\"", "500", "\"YEAR\"")))
			.andExpect(status().isBadRequest());

		verify(choreService, never()).createChore(any(), any());
	}
}
