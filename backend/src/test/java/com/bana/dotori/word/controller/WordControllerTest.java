package com.bana.dotori.word.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
import com.bana.dotori.word.dto.WordDetailResponse;
import com.bana.dotori.word.dto.WordListResponse;
import com.bana.dotori.word.dto.WordSearchResponse;
import com.bana.dotori.word.dto.WordSearchSummary;
import com.bana.dotori.word.dto.WordSummary;
import com.bana.dotori.word.entity.WordCategory;
import com.bana.dotori.word.service.WordService;

@WebMvcTest(WordController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class WordControllerTest {

	private static final String WORDS_URL = "/api/v1/words";
	private static final String BEARER = "Bearer valid-access-token";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WordService wordService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated() {
		given(jwtTokenProvider.getAuthentication("valid-access-token"))
			.willReturn(new UsernamePasswordAuthenticationToken(1, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	@Test
	@DisplayName("단어 목록 조회 성공 시 200과 명세 형식(페이지 정보 포함)의 목록을 반환한다")
	void getWords_success() throws Exception {
		givenAuthenticated();
		given(wordService.getWords(eq(null), anyInt(), anyInt())).willReturn(new WordListResponse(
			List.of(new WordSummary(1, "적금", 1, WordCategory.SAVINGS)), 0, 1));

		mockMvc.perform(get(WORDS_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.words[0].wordId").value(1))
			.andExpect(jsonPath("$.words[0].word").value("적금"))
			.andExpect(jsonPath("$.words[0].difficultyLevel").value(1))
			.andExpect(jsonPath("$.words[0].category").value("SAVINGS"))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.totalPages").value(1));
	}

	@Test
	@DisplayName("category 쿼리 파라미터로 필터링해 조회한다")
	void getWords_filteredByCategory() throws Exception {
		givenAuthenticated();
		given(wordService.getWords(eq(WordCategory.BANKING), anyInt(), anyInt())).willReturn(new WordListResponse(
			List.of(new WordSummary(2, "복리", 3, WordCategory.BANKING)), 0, 1));

		mockMvc.perform(get(WORDS_URL + "?category=BANKING").header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.words[0].category").value("BANKING"));
	}

	@Test
	@DisplayName("토큰 없이 호출하면 401과 UNAUTHORIZED 에러를 반환한다")
	void getWords_unauthorized() throws Exception {
		mockMvc.perform(get(WORDS_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("단어 상세 조회 성공 시 200과 명세 형식(definition 필드)의 상세를 반환한다")
	void getWord_success() throws Exception {
		givenAuthenticated();
		given(wordService.getWord(1)).willReturn(
			new WordDetailResponse(1, "복리", "이자에 다시 이자가 붙는 계산 방식이에요.", 3, WordCategory.BANKING));

		mockMvc.perform(get(WORDS_URL + "/1").header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.wordId").value(1))
			.andExpect(jsonPath("$.word").value("복리"))
			.andExpect(jsonPath("$.definition").value("이자에 다시 이자가 붙는 계산 방식이에요."))
			.andExpect(jsonPath("$.difficultyLevel").value(3))
			.andExpect(jsonPath("$.category").value("BANKING"));
	}

	@Test
	@DisplayName("없는 단어를 상세 조회하면 404와 WORD_NOT_FOUND 에러를 반환한다")
	void getWord_notFound() throws Exception {
		givenAuthenticated();
		given(wordService.getWord(99)).willThrow(new BusinessException(ErrorCode.WORD_NOT_FOUND));

		mockMvc.perform(get(WORDS_URL + "/99").header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("WORD_NOT_FOUND"));
	}

	@Test
	@DisplayName("키워드로 단어 검색 성공 시 200과 명세 형식(definition 필드)의 목록을 반환한다")
	void searchWords_success() throws Exception {
		givenAuthenticated();
		given(wordService.searchWords("이자")).willReturn(new WordSearchResponse(
			List.of(new WordSearchSummary(1, "이자", "돈을 맡기거나 빌려줘서 받는 보상", 1))));

		mockMvc.perform(get(WORDS_URL + "/search?keyword=이자").header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.words[0].wordId").value(1))
			.andExpect(jsonPath("$.words[0].word").value("이자"))
			.andExpect(jsonPath("$.words[0].definition").value("돈을 맡기거나 빌려줘서 받는 보상"))
			.andExpect(jsonPath("$.words[0].difficultyLevel").value(1));
	}

	@Test
	@DisplayName("keyword 없이 검색하면 400과 INVALID_REQUEST 에러를 반환한다")
	void searchWords_missingKeyword() throws Exception {
		givenAuthenticated();
		given(wordService.searchWords(null)).willThrow(new BusinessException(ErrorCode.INVALID_REQUEST));

		mockMvc.perform(get(WORDS_URL + "/search").header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}
}
