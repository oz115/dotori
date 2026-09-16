package com.bana.dotori.word.dto;

import java.util.List;

public record WordListResponse(
	List<WordSummary> words,
	int page,
	int totalPages
) {
}
