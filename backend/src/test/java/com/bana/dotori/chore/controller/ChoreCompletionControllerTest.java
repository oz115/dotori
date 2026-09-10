package com.bana.dotori.chore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.bana.dotori.chore.dto.ChoreApprovalResponse;
import com.bana.dotori.chore.dto.ChoreCompletionHistoryItem;
import com.bana.dotori.chore.dto.ChoreCompletionHistoryResponse;
import com.bana.dotori.chore.dto.ChoreCompletionListResponse;
import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.dto.ChoreCompletionSummary;
import com.bana.dotori.chore.dto.ChoreRejectionResponse;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;
import com.bana.dotori.chore.service.ChoreApprovalService;
import com.bana.dotori.chore.service.ChoreCompletionHistoryService;
import com.bana.dotori.chore.service.ChoreCompletionService;
import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;

@WebMvcTest(ChoreCompletionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class ChoreCompletionControllerTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final Integer CHORE_ID = 7;
	private static final Integer REQUEST_ID = 100;
	private static final String REQUESTS_URL = "/api/v1/families/10/children/2/chores/7/completion-requests";
	private static final String COMPLETIONS_URL = "/api/v1/families/10/children/2/chores/completions";
	private static final String LIST_URL = "/api/v1/families/10/children/2/chores/completion-requests";
	private static final String APPROVE_URL =
		"/api/v1/families/10/children/2/chores/completion-requests/100/approve";
	private static final String REJECT_URL =
		"/api/v1/families/10/children/2/chores/completion-requests/100/reject";
	private static final String CANCEL_URL = "/api/v1/families/10/children/2/chores/completion-requests/100";
	private static final String TOKEN = "access-token";
	private static final String BEARER = "Bearer " + TOKEN;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChoreCompletionService choreCompletionService;

	@MockitoBean
	private ChoreApprovalService choreApprovalService;

	@MockitoBean
	private ChoreCompletionHistoryService choreCompletionHistoryService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated() {
		given(jwtTokenProvider.getAuthentication(TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(CHILD_ID, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	private ChoreCompletionSummary summary() {
		return new ChoreCompletionSummary(REQUEST_ID, CHORE_ID, "설거지", "김아이", null, "다 했어요",
			ChoreCompletionStatus.PENDING, OffsetDateTime.of(2026, 9, 9, 21, 0, 0, 0, ZoneOffset.ofHours(9)));
	}

	private ChoreCompletionResponse response() {
		return new ChoreCompletionResponse(REQUEST_ID, CHORE_ID, ChoreCompletionStatus.PENDING,
			OffsetDateTime.of(2026, 9, 9, 21, 0, 0, 0, ZoneOffset.ofHours(9)));
	}

	private String body(String description) {
		return """
			{
			  "photoKey": "chores/10/2/550e8400-e29b-41d4-a716-446655440000.jpg",
			  "description": %s
			}
			""".formatted(description);
	}

	@Test
	@DisplayName("대기함을 조회하면 200과 목록을 반환한다")
	void getCompletionRequests_ok() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.getCompletionRequests(any(), any(), any(), any()))
			.willReturn(new ChoreCompletionListResponse(List.of(summary())));

		mockMvc.perform(get(LIST_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requests[0].choreCompletionRequestId").value(REQUEST_ID))
			.andExpect(jsonPath("$.requests[0].choreTitle").value("설거지"))
			.andExpect(jsonPath("$.requests[0].childName").value("김아이"))
			.andExpect(jsonPath("$.requests[0].status").value("PENDING"));
	}

	@Test
	@DisplayName("status 를 생략하면 PENDING 으로 조회한다")
	void getCompletionRequests_defaultsToPending() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.getCompletionRequests(any(), any(), any(), any()))
			.willReturn(new ChoreCompletionListResponse(List.of()));

		mockMvc.perform(get(LIST_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk());

		// 대기함은 처리할 것을 보러 오는 화면이라 기본값이 PENDING 이어야 한다.
		verify(choreCompletionService)
			.getCompletionRequests(CHILD_ID, FAMILY_ID, CHILD_ID, ChoreCompletionStatus.PENDING);
	}

	@Test
	@DisplayName("status 를 주면 그 값으로 조회한다")
	void getCompletionRequests_withStatus() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.getCompletionRequests(any(), any(), any(), any()))
			.willReturn(new ChoreCompletionListResponse(List.of()));

		mockMvc.perform(get(LIST_URL)
				.param("status", "SUCCEED")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk());

		verify(choreCompletionService)
			.getCompletionRequests(CHILD_ID, FAMILY_ID, CHILD_ID, ChoreCompletionStatus.SUCCEED);
	}

	@Test
	@DisplayName("없는 상태 이름을 주면 400을 반환한다")
	void getCompletionRequests_unknownStatus() throws Exception {
		givenAuthenticated();

		// 노션 명세에는 APPROVED 로 적혀 있지만 DB ENUM 은 SUCCEED 다. 오타가 조용히
		// 통과하면 빈 목록이 나가고 프론트는 원인을 못 찾는다.
		mockMvc.perform(get(LIST_URL)
				.param("status", "APPROVED")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isBadRequest());

		verify(choreCompletionService, never()).getCompletionRequests(any(), any(), any(), any());
	}

	@Test
	@DisplayName("아이가 대기함을 조회하면 403을 반환한다")
	void getCompletionRequests_forbidden() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.getCompletionRequests(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(get(LIST_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("완료 내역을 조회하면 200과 목록을 반환한다")
	void getCompletions_ok() throws Exception {
		givenAuthenticated();
		given(choreCompletionHistoryService.getCompletions(any(), any(), any(), any(), any()))
			.willReturn(new ChoreCompletionHistoryResponse(List.of(
				new ChoreCompletionHistoryItem(CHORE_ID, "설거지", "김아이", 500,
					OffsetDateTime.of(2026, 9, 9, 21, 0, 0, 0, ZoneOffset.ofHours(9))))));

		mockMvc.perform(get(COMPLETIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completions[0].choreId").value(CHORE_ID))
			.andExpect(jsonPath("$.completions[0].choreTitle").value("설거지"))
			.andExpect(jsonPath("$.completions[0].childName").value("김아이"))
			.andExpect(jsonPath("$.completions[0].rewardPoints").value(500));
	}

	@Test
	@DisplayName("기간을 주면 날짜로 변환해 넘긴다")
	void getCompletions_withPeriod() throws Exception {
		givenAuthenticated();
		given(choreCompletionHistoryService.getCompletions(any(), any(), any(), any(), any()))
			.willReturn(new ChoreCompletionHistoryResponse(List.of()));

		mockMvc.perform(get(COMPLETIONS_URL)
				.param("startDate", "2026-09-01")
				.param("endDate", "2026-09-08")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk());

		verify(choreCompletionHistoryService).getCompletions(CHILD_ID, FAMILY_ID, CHILD_ID,
			LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8));
	}

	@Test
	@DisplayName("기간을 생략하면 null 로 넘겨 서비스가 기본값을 정한다")
	void getCompletions_withoutPeriod() throws Exception {
		givenAuthenticated();
		given(choreCompletionHistoryService.getCompletions(any(), any(), any(), any(), any()))
			.willReturn(new ChoreCompletionHistoryResponse(List.of()));

		mockMvc.perform(get(COMPLETIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk());

		verify(choreCompletionHistoryService).getCompletions(CHILD_ID, FAMILY_ID, CHILD_ID, null, null);
	}

	@Test
	@DisplayName("날짜 형식이 아니면 400을 반환한다")
	void getCompletions_badDate() throws Exception {
		givenAuthenticated();

		mockMvc.perform(get(COMPLETIONS_URL)
				.param("startDate", "2026/09/01")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isBadRequest());

		verify(choreCompletionHistoryService, never()).getCompletions(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("시작일이 종료일보다 늦으면 400과 INVALID_DATE_RANGE 를 반환한다")
	void getCompletions_invalidRange() throws Exception {
		givenAuthenticated();
		given(choreCompletionHistoryService.getCompletions(any(), any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.INVALID_DATE_RANGE));

		mockMvc.perform(get(COMPLETIONS_URL)
				.param("startDate", "2026-09-08")
				.param("endDate", "2026-09-01")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
	}

	@Test
	@DisplayName("완료를 요청하면 201과 대기 상태를 반환한다")
	void requestCompletion_created() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.requestCompletion(any(), any(), any(), any(), any())).willReturn(response());

		mockMvc.perform(post(REQUESTS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("\"다 했어요\"")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.choreCompletionRequestId").value(REQUEST_ID))
			.andExpect(jsonPath("$.choreId").value(CHORE_ID))
			.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	@DisplayName("토큰이 없으면 401을 반환한다")
	void requestCompletion_unauthorized() throws Exception {
		mockMvc.perform(post(REQUESTS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("\"다 했어요\"")))
			.andExpect(status().isUnauthorized());

		verify(choreCompletionService, never()).requestCompletion(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("사진 키가 100자를 넘으면 400을 반환하고 서비스를 호출하지 않는다")
	void requestCompletion_photoKeyTooLong() throws Exception {
		givenAuthenticated();
		String tooLong = "h".repeat(101);

		mockMvc.perform(post(REQUESTS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"photoKey\": \"" + tooLong + "\"}"))
			.andExpect(status().isBadRequest());

		verify(choreCompletionService, never()).requestCompletion(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("부모가 요청하면 403을 반환한다")
	void requestCompletion_forbidden() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.requestCompletion(any(), any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(post(REQUESTS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("null")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("이미 대기 중인 요청이 있으면 409와 PENDING_REQUEST_EXISTS 를 반환한다")
	void requestCompletion_pendingExists() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.requestCompletion(any(), any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.PENDING_REQUEST_EXISTS));

		mockMvc.perform(post(REQUESTS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("null")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PENDING_REQUEST_EXISTS"));
	}

	@Test
	@DisplayName("보상이 산정되지 않았으면 400과 REWARD_NOT_EVALUATED 를 반환한다")
	void requestCompletion_rewardNotEvaluated() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.requestCompletion(any(), any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.REWARD_NOT_EVALUATED));

		mockMvc.perform(post(REQUESTS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("null")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("REWARD_NOT_EVALUATED"));
	}

	@Test
	@DisplayName("승인하면 200과 지급된 보상을 반환한다")
	void approveCompletion_ok() throws Exception {
		givenAuthenticated();
		given(choreApprovalService.approve(any(), any(), any(), any()))
			.willReturn(new ChoreApprovalResponse(REQUEST_ID, ChoreCompletionStatus.SUCCEED, 500,
				OffsetDateTime.of(2026, 9, 9, 21, 0, 0, 0, ZoneOffset.ofHours(9))));

		mockMvc.perform(post(APPROVE_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.choreCompletionRequestId").value(REQUEST_ID))
			.andExpect(jsonPath("$.status").value("SUCCEED"))
			.andExpect(jsonPath("$.rewardPoints").value(500));

		verify(choreApprovalService).approve(CHILD_ID, FAMILY_ID, CHILD_ID, REQUEST_ID);
	}

	@Test
	@DisplayName("이미 처리된 요청을 승인하려 하면 409와 INVALID_STATUS 를 반환한다")
	void approveCompletion_invalidStatus() throws Exception {
		givenAuthenticated();
		given(choreApprovalService.approve(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.INVALID_STATUS));

		mockMvc.perform(post(APPROVE_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS"));
	}

	@Test
	@DisplayName("이번 주기에 이미 지급됐으면 409와 ALREADY_REWARDED 를 반환한다")
	void approveCompletion_alreadyRewarded() throws Exception {
		givenAuthenticated();
		given(choreApprovalService.approve(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.ALREADY_REWARDED));

		mockMvc.perform(post(APPROVE_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_REWARDED"));
	}

	@Test
	@DisplayName("아이가 승인하려 하면 403을 반환한다")
	void approveCompletion_forbidden() throws Exception {
		givenAuthenticated();
		given(choreApprovalService.approve(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(post(APPROVE_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("승인에 토큰이 없으면 401을 반환한다")
	void approveCompletion_unauthorized() throws Exception {
		mockMvc.perform(post(APPROVE_URL))
			.andExpect(status().isUnauthorized());

		verify(choreApprovalService, never()).approve(any(), any(), any(), any());
	}

	@Test
	@DisplayName("거절하면 200과 REJECTED 를 반환한다")
	void rejectCompletion_ok() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.rejectCompletion(any(), any(), any(), any()))
			.willReturn(new ChoreRejectionResponse(REQUEST_ID, ChoreCompletionStatus.REJECTED,
				OffsetDateTime.of(2026, 9, 9, 21, 0, 0, 0, ZoneOffset.ofHours(9))));

		// 본문이 없다. 명세의 reason 은 담을 곳이 없어 팀 결정으로 뺐다.
		mockMvc.perform(post(REJECT_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.choreCompletionRequestId").value(REQUEST_ID))
			.andExpect(jsonPath("$.status").value("REJECTED"))
			.andExpect(jsonPath("$.processedAt").exists());

		verify(choreCompletionService).rejectCompletion(CHILD_ID, FAMILY_ID, CHILD_ID, REQUEST_ID);
	}

	@Test
	@DisplayName("이미 처리된 요청을 거절하려 하면 409와 INVALID_STATUS 를 반환한다")
	void rejectCompletion_invalidStatus() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.rejectCompletion(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.INVALID_STATUS));

		mockMvc.perform(post(REJECT_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS"));
	}

	@Test
	@DisplayName("아이가 거절하려 하면 403을 반환한다")
	void rejectCompletion_forbidden() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.rejectCompletion(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(post(REJECT_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("없는 요청을 거절하려 하면 404를 반환한다")
	void rejectCompletion_notFound() throws Exception {
		givenAuthenticated();
		given(choreCompletionService.rejectCompletion(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND));

		mockMvc.perform(post(REJECT_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CHORE_COMPLETION_REQUEST_NOT_FOUND"));
	}

	@Test
	@DisplayName("거절에 토큰이 없으면 401을 반환한다")
	void rejectCompletion_unauthorized() throws Exception {
		mockMvc.perform(post(REJECT_URL))
			.andExpect(status().isUnauthorized());

		verify(choreCompletionService, never()).rejectCompletion(any(), any(), any(), any());
	}

	@Test
	@DisplayName("요청을 취소하면 204를 반환한다")
	void cancelCompletion_noContent() throws Exception {
		givenAuthenticated();

		mockMvc.perform(delete(CANCEL_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNoContent());

		verify(choreCompletionService).cancelCompletion(CHILD_ID, FAMILY_ID, CHILD_ID, REQUEST_ID);
	}

	@Test
	@DisplayName("남의 요청을 취소하려 하면 403을 반환한다")
	void cancelCompletion_forbidden() throws Exception {
		givenAuthenticated();
		willThrow(new BusinessException(ErrorCode.FORBIDDEN))
			.given(choreCompletionService).cancelCompletion(any(), any(), any(), any());

		mockMvc.perform(delete(CANCEL_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("이미 처리된 요청을 취소하려 하면 409와 INVALID_STATUS 를 반환한다")
	void cancelCompletion_invalidStatus() throws Exception {
		givenAuthenticated();
		willThrow(new BusinessException(ErrorCode.INVALID_STATUS))
			.given(choreCompletionService).cancelCompletion(any(), any(), any(), any());

		mockMvc.perform(delete(CANCEL_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS"));
	}

	@Test
	@DisplayName("없는 요청을 취소하려 하면 404를 반환한다")
	void cancelCompletion_notFound() throws Exception {
		givenAuthenticated();
		willThrow(new BusinessException(ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND))
			.given(choreCompletionService).cancelCompletion(any(), any(), any(), any());

		mockMvc.perform(delete(CANCEL_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CHORE_COMPLETION_REQUEST_NOT_FOUND"));
	}
}
