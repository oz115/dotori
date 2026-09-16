package com.bana.dotori.word.dto;

import com.bana.dotori.word.entity.WordCategory;

/**
 * 경제 단어 상세. 지금은 목록(WordSummary)과 필드가 같지만, 상세 화면 전용 정보가
 * 늘어날 걸 대비해 상점(StoreProductDetailResponse)·투자처럼 타입을 분리해 둔다.
 */
public record WordDetailResponse(
	Integer wordId,
	String word,
	String definition,
	Integer difficultyLevel,
	WordCategory category
) {
}
