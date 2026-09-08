package com.bana.dotori.chore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.bana.dotori.chore.dto.ChoreListResponse;
import com.bana.dotori.chore.dto.ChoreSummary;
import com.bana.dotori.chore.dto.ChoreUpdateRequest;
import com.bana.dotori.chore.entity.ChoreDifficulty;
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

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final Integer USER_ID = 1;
	private static final Integer CHORE_ID = 7;
	private static final String CHORES_URL = "/api/v1/families/10/children/2/chores";
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

	private String body(String title, String difficulty, String repeatType) {
		return """
			{
			  "title": %s,
			  "description": "저녁 먹고 나서",
			  "difficulty": %s,
			  "repeatType": %s
			}
			""".formatted(title, difficulty, repeatType);
	}

	private String validBody() {
		return body("\"설거지\"", "\"NORMAL\"", "\"DAY\"");
	}

	private ChoreDetailResponse detail() {
		return new ChoreDetailResponse(CHORE_ID, FAMILY_ID, USER_ID, CHILD_ID, "설거지", "저녁 먹고 나서",
			ChoreDifficulty.NORMAL, new BigDecimal("0.50"), 500, ChoreRepeatType.DAY,
			LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
			OffsetDateTime.of(2026, 9, 8, 9, 0, 0, 0, ZoneOffset.ofHours(9)), null);
	}

	@Test
	@DisplayName("목록 조회 성공 시 200과 그 아이의 집안일 목록을 반환한다")
	void getChores_success() throws Exception {
		givenAuthenticated(USER_ID);
		OffsetDateTime createdAt = OffsetDateTime.of(2026, 9, 8, 9, 0, 0, 0, ZoneOffset.ofHours(9));
		given(choreService.getChores(USER_ID, FAMILY_ID, CHILD_ID)).willReturn(new ChoreListResponse(List.of(
			new ChoreSummary(CHORE_ID, CHILD_ID, "설거지", ChoreDifficulty.NORMAL, 500, ChoreRepeatType.DAY,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), createdAt),
			new ChoreSummary(8, CHILD_ID, "방 청소", ChoreDifficulty.EASY, 300, ChoreRepeatType.WEEK,
				LocalDate.of(2026, 9, 1), null, createdAt))));

		mockMvc.perform(get(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.chores.length()").value(2))
			.andExpect(jsonPath("$.chores[0].choreId").value(CHORE_ID))
			.andExpect(jsonPath("$.chores[0].childId").value(CHILD_ID))
			.andExpect(jsonPath("$.chores[0].title").value("설거지"))
			.andExpect(jsonPath("$.chores[0].repeatType").value("DAY"))
			.andExpect(jsonPath("$.chores[1].endDate").doesNotExist());
	}

	@Test
	@DisplayName("경로의 familyId·childId를 그대로 서비스에 넘긴다")
	void getChores_passesPathVariables() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.getChores(USER_ID, FAMILY_ID, CHILD_ID)).willReturn(new ChoreListResponse(List.of()));

		mockMvc.perform(get(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk());

		verify(choreService).getChores(USER_ID, FAMILY_ID, CHILD_ID);
	}

	@Test
	@DisplayName("소속되지 않은 가족이면 404와 FAMILY_NOT_FOUND 에러를 반환한다")
	void getChores_familyNotFound() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.getChores(USER_ID, FAMILY_ID, CHILD_ID))
			.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		mockMvc.perform(get(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FAMILY_NOT_FOUND"));
	}

	@Test
	@DisplayName("그 가족의 자녀가 아니면 404와 MEMBER_NOT_FOUND 에러를 반환한다")
	void getChores_memberNotFound() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.getChores(USER_ID, FAMILY_ID, CHILD_ID))
			.willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		mockMvc.perform(get(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}

	@Test
	@DisplayName("아이가 형제 것을 조회하면 403을 반환한다")
	void getChores_forbidden() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.getChores(USER_ID, FAMILY_ID, CHILD_ID))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(get(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("토큰 없이 목록을 조회하면 401을 반환하고 서비스는 호출하지 않는다")
	void getChores_unauthorized() throws Exception {
		mockMvc.perform(get(CHORES_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		verify(choreService, never()).getChores(any(), any(), any());
	}

	@Test
	@DisplayName("상세 조회 성공 시 200과 상세 정보를 반환한다")
	void getChore_success() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.getChore(USER_ID, FAMILY_ID, CHILD_ID, CHORE_ID)).willReturn(detail());

		mockMvc.perform(get(CHORES_URL + "/" + CHORE_ID)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.choreId").value(CHORE_ID))
			.andExpect(jsonPath("$.description").value("저녁 먹고 나서"))
			.andExpect(jsonPath("$.deletedAt").doesNotExist());
	}

	@Test
	@DisplayName("없거나 경로와 담당이 어긋나는 집안일이면 404와 CHORE_NOT_FOUND 에러를 반환한다")
	void getChore_notFound() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.getChore(USER_ID, FAMILY_ID, CHILD_ID, CHORE_ID))
			.willThrow(new BusinessException(ErrorCode.CHORE_NOT_FOUND));

		mockMvc.perform(get(CHORES_URL + "/" + CHORE_ID)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CHORE_NOT_FOUND"));
	}

	@Test
	@DisplayName("생성 성공 시 201과 생성된 집안일을 반환한다")
	void createChore_success() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.createChore(eq(USER_ID), eq(FAMILY_ID), eq(CHILD_ID), any(ChoreCreateRequest.class)))
			.willReturn(detail());

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBody()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.choreId").value(CHORE_ID))
			.andExpect(jsonPath("$.familyId").value(FAMILY_ID))
			.andExpect(jsonPath("$.parentId").value(USER_ID))
			.andExpect(jsonPath("$.childId").value(CHILD_ID))
			.andExpect(jsonPath("$.title").value("설거지"))
			.andExpect(jsonPath("$.rewardPoints").value(500))
			.andExpect(jsonPath("$.repeatType").value("DAY"))
			.andExpect(jsonPath("$.startDate").value("2026-09-01"))
			.andExpect(jsonPath("$.endDate").value("2026-09-30"))
			.andExpect(jsonPath("$.deletedAt").doesNotExist());
	}

	@Test
	@DisplayName("토큰 없이 생성하면 401을 반환하고 서비스는 호출하지 않는다")
	void createChore_unauthorized() throws Exception {
		mockMvc.perform(post(CHORES_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBody()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		verify(choreService, never()).createChore(any(), any(), any(), any());
	}

	@Test
	@DisplayName("아이가 생성을 호출하면 403과 FORBIDDEN 에러를 반환한다")
	void createChore_forbidden() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.createChore(eq(USER_ID), eq(FAMILY_ID), eq(CHILD_ID), any(ChoreCreateRequest.class)))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBody()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("필수 값이 빠지면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출하지 않는다")
	void createChore_invalidRequest() throws Exception {
		givenAuthenticated(USER_ID);

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("\" \"", "\"NORMAL\"", "\"DAY\"")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		verify(choreService, never()).createChore(any(), any(), any(), any());
	}

	@Test
	@DisplayName("보상은 요청에 담을 수 없다 — 보내도 무시되고 AI가 정한다")
	void createChore_ignoresReward() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.createChore(eq(USER_ID), eq(FAMILY_ID), eq(CHILD_ID), any(ChoreCreateRequest.class)))
			.willReturn(detail());

		// 요청 스펙에 없는 필드라 역직렬화 단계에서 버려진다. 부모가 보상을 정하지 못하게 막는다.
		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "title": "설거지",
					  "difficulty": "NORMAL",
					  "repeatType": "DAY",
					  "rewardPoints": 999999
					}
					"""))
			.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("없는 난이도를 보내면 400을 반환한다")
	void createChore_unknownDifficulty() throws Exception {
		givenAuthenticated(USER_ID);

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("\"설거지\"", "\"EXTREME\"", "\"DAY\"")))
			.andExpect(status().isBadRequest());

		verify(choreService, never()).createChore(any(), any(), any(), any());
	}

	@Test
	@DisplayName("없는 반복 주기를 보내면 400을 반환한다")
	void createChore_unknownRepeatType() throws Exception {
		givenAuthenticated(USER_ID);

		mockMvc.perform(post(CHORES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("\"설거지\"", "\"NORMAL\"", "\"YEAR\"")))
			.andExpect(status().isBadRequest());

		verify(choreService, never()).createChore(any(), any(), any(), any());
	}

	@Test
	@DisplayName("수정 성공 시 200과 수정된 집안일을 반환한다")
	void updateChore_success() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.updateChore(eq(USER_ID), eq(FAMILY_ID), eq(CHILD_ID), eq(CHORE_ID),
			any(ChoreUpdateRequest.class))).willReturn(detail());

		mockMvc.perform(patch(CHORES_URL + "/" + CHORE_ID)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rewardPoints\": 700}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.choreId").value(CHORE_ID));
	}

	@Test
	@DisplayName("수정 요청이 비어 있어도 200이다")
	void updateChore_emptyBody() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.updateChore(eq(USER_ID), eq(FAMILY_ID), eq(CHILD_ID), eq(CHORE_ID),
			any(ChoreUpdateRequest.class))).willReturn(detail());

		// PATCH 라서 모든 필드가 선택이다. 빈 본문이 400 이 되면 안 된다.
		mockMvc.perform(patch(CHORES_URL + "/" + CHORE_ID)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("수정에서 title이 공백이면 400을 반환하고 서비스는 호출하지 않는다")
	void updateChore_blankTitle() throws Exception {
		givenAuthenticated(USER_ID);

		mockMvc.perform(patch(CHORES_URL + "/" + CHORE_ID)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\": \" \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		verify(choreService, never()).updateChore(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("아이가 수정을 호출하면 403을 반환한다")
	void updateChore_forbidden() throws Exception {
		givenAuthenticated(USER_ID);
		given(choreService.updateChore(eq(USER_ID), eq(FAMILY_ID), eq(CHILD_ID), eq(CHORE_ID),
			any(ChoreUpdateRequest.class))).willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(patch(CHORES_URL + "/" + CHORE_ID)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rewardPoints\": 700}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("삭제 성공 시 204를 반환하고 본문이 없다")
	void deleteChore_success() throws Exception {
		givenAuthenticated(USER_ID);

		mockMvc.perform(delete(CHORES_URL + "/" + CHORE_ID)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNoContent());

		verify(choreService).deleteChore(USER_ID, FAMILY_ID, CHILD_ID, CHORE_ID);
	}

	@Test
	@DisplayName("없는 집안일을 삭제하면 404를 반환한다")
	void deleteChore_notFound() throws Exception {
		givenAuthenticated(USER_ID);
		willThrow(new BusinessException(ErrorCode.CHORE_NOT_FOUND))
			.given(choreService).deleteChore(USER_ID, FAMILY_ID, CHILD_ID, CHORE_ID);

		mockMvc.perform(delete(CHORES_URL + "/" + CHORE_ID)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CHORE_NOT_FOUND"));
	}

	@Test
	@DisplayName("토큰 없이 삭제하면 401을 반환하고 서비스는 호출하지 않는다")
	void deleteChore_unauthorized() throws Exception {
		mockMvc.perform(delete(CHORES_URL + "/" + CHORE_ID))
			.andExpect(status().isUnauthorized());

		verify(choreService, never()).deleteChore(any(), any(), any(), any());
	}
}
