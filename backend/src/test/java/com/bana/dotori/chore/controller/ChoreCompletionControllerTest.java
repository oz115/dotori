package com.bana.dotori.chore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;
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
	private static final String CANCEL_URL = "/api/v1/families/10/children/2/chores/completion-requests/100";
	private static final String TOKEN = "access-token";
	private static final String BEARER = "Bearer " + TOKEN;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChoreCompletionService choreCompletionService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated() {
		given(jwtTokenProvider.getAuthentication(TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(CHILD_ID, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	private ChoreCompletionResponse response() {
		return new ChoreCompletionResponse(REQUEST_ID, CHORE_ID, ChoreCompletionStatus.PENDING,
			OffsetDateTime.of(2026, 9, 9, 21, 0, 0, 0, ZoneOffset.ofHours(9)));
	}

	private String body(String description) {
		return """
			{
			  "photoUrl": null,
			  "description": %s
			}
			""".formatted(description);
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
	@DisplayName("사진 URL이 100자를 넘으면 400을 반환하고 서비스를 호출하지 않는다")
	void requestCompletion_photoUrlTooLong() throws Exception {
		givenAuthenticated();
		String tooLong = "h".repeat(101);

		mockMvc.perform(post(REQUESTS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"photoUrl\": \"" + tooLong + "\"}"))
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
