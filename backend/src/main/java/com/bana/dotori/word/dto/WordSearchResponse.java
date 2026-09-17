package com.bana.dotori.word.dto;

import java.util.List;

public record WordSearchResponse(
	List<WordSearchSummary> words
) {
}
