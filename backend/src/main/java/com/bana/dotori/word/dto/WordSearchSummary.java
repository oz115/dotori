package com.bana.dotori.word.dto;

/** 단어 검색 결과의 한 건 (WORD-03). */
public record WordSearchSummary(
	Integer wordId,
	String word,
	String definition,
	Integer difficultyLevel
) {
}
