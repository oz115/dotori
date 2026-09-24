package com.bana.dotori.word.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.word.dto.WordDetailResponse;
import com.bana.dotori.word.dto.WordListResponse;
import com.bana.dotori.word.dto.WordSearchResponse;
import com.bana.dotori.word.entity.Word;
import com.bana.dotori.word.entity.WordCategory;
import com.bana.dotori.word.mapper.WordMapper;
import com.bana.dotori.word.repository.WordRepository;

import lombok.RequiredArgsConstructor;

/**
 * 경제 단어장 조회. 가족·자녀 구분이 없는 전역 사전이라 상점·투자와 달리 소속 확인이 없다.
 */
@Service
@RequiredArgsConstructor
public class WordServiceImpl implements WordService {

	private final WordRepository wordRepository;
	private final WordMapper wordMapper;

	@Override
	@Transactional(readOnly = true)
	public WordListResponse getWords(WordCategory category, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Word> words = category == null
			? wordRepository.findAllByOrderByWordAscIdAsc(pageable)
			: wordRepository.findAllByCategoryOrderByWordAscIdAsc(category, pageable);
		return wordMapper.toListResponse(words);
	}

	@Override
	@Transactional(readOnly = true)
	public WordDetailResponse getWord(Integer wordId) {
		Word word = wordRepository.findById(wordId)
			.orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));
		return wordMapper.toDetailResponse(word);
	}

	@Override
	@Transactional(readOnly = true)
	public WordSearchResponse searchWords(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		List<Word> words = wordRepository.findByWordContainingIgnoreCaseOrderByWordAscIdAsc(keyword);
		return wordMapper.toSearchResponse(words);
	}
}
