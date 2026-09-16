package com.bana.dotori.word.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.word.dto.WordDetailResponse;
import com.bana.dotori.word.dto.WordListResponse;
import com.bana.dotori.word.dto.WordSearchResponse;
import com.bana.dotori.word.entity.WordCategory;
import com.bana.dotori.word.service.WordService;

import lombok.RequiredArgsConstructor;

/**
 * 경제 단어장(WORD). 가족·자녀 구분이 없는 전역 사전이라 조회 결과가 사용자마다 다르지
 * 않는다 — 인증은 SecurityConfig의 anyRequest().authenticated() 로만 걸고, principal 은
 * 여기서 쓰지 않는다.
 */
@RestController
@RequestMapping("/api/v1/words")
@RequiredArgsConstructor
public class WordController {

	private final WordService wordService;

	@GetMapping
	public ResponseEntity<WordListResponse> getWords(
		@RequestParam(required = false) WordCategory category,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(wordService.getWords(category, page, size));
	}

	@GetMapping("/search")
	public ResponseEntity<WordSearchResponse> searchWords(@RequestParam(required = false) String keyword) {
		return ResponseEntity.ok(wordService.searchWords(keyword));
	}

	@GetMapping("/{wordId}")
	public ResponseEntity<WordDetailResponse> getWord(@PathVariable Integer wordId) {
		return ResponseEntity.ok(wordService.getWord(wordId));
	}
}
