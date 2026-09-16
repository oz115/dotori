package com.bana.dotori.word.dto;

import com.bana.dotori.word.entity.WordCategory;

/** 경제 단어장 목록의 한 건. 뜻(definition)은 상세 조회에서만 내려준다 (API 명세 기준). */
public record WordSummary(
	Integer wordId,
	String word,
	Integer difficultyLevel,
	WordCategory category
) {
}
