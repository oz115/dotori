package com.bana.dotori.quiz.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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

import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.quiz.dto.QuizAttemptHistoryItem;
import com.bana.dotori.quiz.dto.QuizHistoryResponse;
import com.bana.dotori.quiz.dto.QuizParentAttemptItem;
import com.bana.dotori.quiz.dto.QuizParentTodayResponse;
import com.bana.dotori.quiz.dto.QuizSubmitResponse;
import com.bana.dotori.quiz.dto.QuizTodayResponse;
import com.bana.dotori.quiz.service.QuizService;

@WebMvcTest(QuizController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class QuizControllerTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final Integer QUIZ_ID = 100;
	private static final String TODAY_URL = "/api/v1/families/10/children/2/quizzes/today";
	private static final String REVIEW_URL = "/api/v1/families/10/children/2/quizzes/today/review";
	private static final String HISTORY_URL = "/api/v1/families/10/children/2/quizzes/history";
	private static final String SUBMIT_URL = "/api/v1/families/10/children/2/quizzes/100/submit";
	private static final String TOKEN = "access-token";
	private static final String BEARER = "Bearer " + TOKEN;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private QuizService quizService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated() {
		given(jwtTokenProvider.getAuthentication(TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(CHILD_ID, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	@Test
	@DisplayName("오늘의 퀴즈를 조회하면 200과 퀴즈를 반환한다")
	void getTodayQuiz_ok() throws Exception {
		givenAuthenticated();
		given(quizService.getTodayQuiz(any(), any(), any())).willReturn(new QuizTodayResponse(
			QUIZ_ID, LocalDate.of(2026, 9, 16), "'이자'란 무엇일까요?", null, 100, false, null, null, 0, null));

		mockMvc.perform(get(TODAY_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.quizId").value(QUIZ_ID))
			.andExpect(jsonPath("$.question").value("'이자'란 무엇일까요?"))
			.andExpect(jsonPath("$.rewardPoints").value(100))
			.andExpect(jsonPath("$.alreadySubmitted").value(false))
			.andExpect(jsonPath("$.correctAnswer").doesNotExist());

		verify(quizService).getTodayQuiz(CHILD_ID, FAMILY_ID, CHILD_ID);
	}

	@Test
	@DisplayName("오늘자 퀴즈가 없으면 404와 QUIZ_NOT_READY 를 반환한다")
	void getTodayQuiz_notReady() throws Exception {
		givenAuthenticated();
		given(quizService.getTodayQuiz(any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.QUIZ_NOT_READY));

		mockMvc.perform(get(TODAY_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("QUIZ_NOT_READY"));
	}

	@Test
	@DisplayName("부모가 조회하면 403을 반환한다")
	void getTodayQuiz_forbidden() throws Exception {
		givenAuthenticated();
		given(quizService.getTodayQuiz(any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(get(TODAY_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("토큰이 없으면 401을 반환한다")
	void getTodayQuiz_unauthorized() throws Exception {
		mockMvc.perform(get(TODAY_URL))
			.andExpect(status().isUnauthorized());

		verify(quizService, never()).getTodayQuiz(any(), any(), any());
	}

	@Test
	@DisplayName("부모가 오늘의 퀴즈를 확인하면 200과 정답·제출 내역을 반환한다")
	void getTodayQuizForParent_ok() throws Exception {
		givenAuthenticated();
		given(quizService.getTodayQuizForParent(any(), any(), any())).willReturn(new QuizParentTodayResponse(
			QUIZ_ID, LocalDate.of(2026, 9, 16), "'이자'란 무엇일까요?", "이자", "설명", 1, 3, false, false,
			List.of(new QuizParentAttemptItem(5, 1, "수수료", false, 0,
				OffsetDateTime.of(2026, 9, 16, 21, 0, 0, 0, java.time.ZoneOffset.ofHours(9))))));

		mockMvc.perform(get(REVIEW_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.quizId").value(QUIZ_ID))
			.andExpect(jsonPath("$.correctAnswer").value("이자"))
			.andExpect(jsonPath("$.maxAttempts").value(3))
			.andExpect(jsonPath("$.locked").value(false))
			.andExpect(jsonPath("$.attempts[0].attemptNumber").value(1))
			.andExpect(jsonPath("$.attempts[0].submittedAnswer").value("수수료"))
			.andExpect(jsonPath("$.attempts[0].isCorrect").value(false));

		verify(quizService).getTodayQuizForParent(CHILD_ID, FAMILY_ID, CHILD_ID);
	}

	@Test
	@DisplayName("아이가 부모용 확인을 부르면 403을 반환한다")
	void getTodayQuizForParent_forbidden() throws Exception {
		givenAuthenticated();
		given(quizService.getTodayQuizForParent(any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(get(REVIEW_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"))
			.andExpect(jsonPath("$.correctAnswer").doesNotExist());
	}

	@Test
	@DisplayName("오늘자 퀴즈가 없으면 부모용 확인도 404와 QUIZ_NOT_READY 를 반환한다")
	void getTodayQuizForParent_notReady() throws Exception {
		givenAuthenticated();
		given(quizService.getTodayQuizForParent(any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.QUIZ_NOT_READY));

		mockMvc.perform(get(REVIEW_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("QUIZ_NOT_READY"));
	}

	@Test
	@DisplayName("토큰이 없으면 부모용 확인도 401을 반환한다")
	void getTodayQuizForParent_unauthorized() throws Exception {
		mockMvc.perform(get(REVIEW_URL))
			.andExpect(status().isUnauthorized());

		verify(quizService, never()).getTodayQuizForParent(any(), any(), any());
	}

	@Test
	@DisplayName("정답을 제출하면 200과 결과를 반환한다")
	void submitQuiz_correct() throws Exception {
		givenAuthenticated();
		given(quizService.submitQuiz(any(), any(), any(), any(), any())).willReturn(
			new QuizSubmitResponse(1, true, "이자", "설명", 100, 17600, null));

		mockMvc.perform(post(SUBMIT_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"answer\": \"이자\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.quizAttemptId").value(1))
			.andExpect(jsonPath("$.isCorrect").value(true))
			.andExpect(jsonPath("$.correctAnswer").value("이자"))
			.andExpect(jsonPath("$.earnedPoints").value(100))
			.andExpect(jsonPath("$.pointBalanceAfter").value(17600));
	}

	@Test
	@DisplayName("오답을 제출하면 정답은 숨기고 초성 힌트를 반환한다")
	void submitQuiz_wrongWithHint() throws Exception {
		givenAuthenticated();
		given(quizService.submitQuiz(any(), any(), any(), any(), any())).willReturn(
			new QuizSubmitResponse(2, false, null, null, 0, 17500, "ㅇㅈ"));

		mockMvc.perform(post(SUBMIT_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"answer\": \"수수료\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isCorrect").value(false))
			.andExpect(jsonPath("$.correctAnswer").doesNotExist())
			.andExpect(jsonPath("$.hint").value("ㅇㅈ"));
	}

	@Test
	@DisplayName("답안이 비어 있으면 400을 반환하고 서비스를 호출하지 않는다")
	void submitQuiz_blankAnswer() throws Exception {
		givenAuthenticated();

		mockMvc.perform(post(SUBMIT_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"answer\": \"  \"}"))
			.andExpect(status().isBadRequest());

		verify(quizService, never()).submitQuiz(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("이미 정답 처리된 퀴즈면 409와 ALREADY_SUBMITTED 를 반환한다")
	void submitQuiz_alreadySubmitted() throws Exception {
		givenAuthenticated();
		given(quizService.submitQuiz(any(), any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.ALREADY_SUBMITTED));

		mockMvc.perform(post(SUBMIT_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"answer\": \"이자\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_SUBMITTED"));
	}

	@Test
	@DisplayName("3회를 다 쓴 뒤 제출하면 409와 QUIZ_ATTEMPT_LIMIT_EXCEEDED 를 반환한다")
	void submitQuiz_attemptLimitExceeded() throws Exception {
		givenAuthenticated();
		given(quizService.submitQuiz(any(), any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.QUIZ_ATTEMPT_LIMIT_EXCEEDED));

		mockMvc.perform(post(SUBMIT_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"answer\": \"이자\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("QUIZ_ATTEMPT_LIMIT_EXCEEDED"));
	}

	@Test
	@DisplayName("없는 퀴즈면 404와 QUIZ_NOT_FOUND 를 반환한다")
	void submitQuiz_notFound() throws Exception {
		givenAuthenticated();
		given(quizService.submitQuiz(any(), any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

		mockMvc.perform(post(SUBMIT_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"answer\": \"이자\"}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("QUIZ_NOT_FOUND"));
	}

	@Test
	@DisplayName("이력을 조회하면 200과 목록을 반환한다")
	void getHistory_ok() throws Exception {
		givenAuthenticated();
		given(quizService.getHistory(any(), any(), any(), any(), any())).willReturn(new QuizHistoryResponse(
			List.of(new QuizAttemptHistoryItem(1, LocalDate.of(2026, 9, 15), "'이자'란?", "이자", true, 100,
				OffsetDateTime.of(2026, 9, 15, 21, 0, 0, 0, java.time.ZoneOffset.ofHours(9))))));

		mockMvc.perform(get(HISTORY_URL)
				.param("startDate", "2026-09-01")
				.param("endDate", "2026-09-16")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.attempts[0].quizAttemptId").value(1))
			.andExpect(jsonPath("$.attempts[0].question").value("'이자'란?"))
			.andExpect(jsonPath("$.attempts[0].submittedAnswer").value("이자"))
			.andExpect(jsonPath("$.attempts[0].isCorrect").value(true))
			.andExpect(jsonPath("$.attempts[0].earnedPoints").value(100));

		verify(quizService).getHistory(CHILD_ID, FAMILY_ID, CHILD_ID,
			LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 16));
	}

	@Test
	@DisplayName("기간을 생략하면 null 로 넘긴다")
	void getHistory_withoutPeriod() throws Exception {
		givenAuthenticated();
		given(quizService.getHistory(any(), any(), any(), any(), any()))
			.willReturn(new QuizHistoryResponse(List.of()));

		mockMvc.perform(get(HISTORY_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk());

		verify(quizService).getHistory(CHILD_ID, FAMILY_ID, CHILD_ID, null, null);
	}
}
