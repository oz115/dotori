package com.bana.dotori.word.service;

import com.bana.dotori.word.dto.WordDetailResponse;
import com.bana.dotori.word.dto.WordListResponse;
import com.bana.dotori.word.dto.WordSearchResponse;
import com.bana.dotori.word.entity.WordCategory;

public interface WordService {

	WordListResponse getWords(WordCategory category, int page, int size);

	WordDetailResponse getWord(Integer wordId);

	WordSearchResponse searchWords(String keyword);
}
