package com.bana.dotori.word.mapper;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.bana.dotori.word.dto.WordDetailResponse;
import com.bana.dotori.word.dto.WordListResponse;
import com.bana.dotori.word.dto.WordSearchResponse;
import com.bana.dotori.word.dto.WordSearchSummary;
import com.bana.dotori.word.dto.WordSummary;
import com.bana.dotori.word.entity.Word;

@Component
public class WordMapper {

	public WordListResponse toListResponse(Page<Word> page) {
		List<WordSummary> summaries = page.getContent().stream()
			.map(this::toSummary)
			.toList();
		return new WordListResponse(summaries, page.getNumber(), page.getTotalPages());
	}

	public WordSummary toSummary(Word word) {
		return new WordSummary(word.getId(), word.getWord(), word.getDifficultyLevel(), word.getCategory());
	}

	public WordDetailResponse toDetailResponse(Word word) {
		return new WordDetailResponse(word.getId(), word.getWord(), word.getDescription(),
			word.getEasyDescription(), word.getDifficultyLevel(), word.getCategory());
	}

	public WordSearchResponse toSearchResponse(List<Word> words) {
		return new WordSearchResponse(words.stream()
			.map(this::toSearchSummary)
			.toList());
	}

	public WordSearchSummary toSearchSummary(Word word) {
		return new WordSearchSummary(word.getId(), word.getWord(), word.getDescription(), word.getDifficultyLevel());
	}
}
