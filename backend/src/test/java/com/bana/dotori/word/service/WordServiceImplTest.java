package com.bana.dotori.word.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.word.dto.WordDetailResponse;
import com.bana.dotori.word.dto.WordListResponse;
import com.bana.dotori.word.dto.WordSearchResponse;
import com.bana.dotori.word.dto.WordSummary;
import com.bana.dotori.word.entity.Word;
import com.bana.dotori.word.entity.WordCategory;
import com.bana.dotori.word.mapper.WordMapper;
import com.bana.dotori.word.repository.WordRepository;

@ExtendWith(MockitoExtension.class)
class WordServiceImplTest {

	@Mock
	private WordRepository wordRepository;

	@Spy
	private WordMapper wordMapper;

	@InjectMocks
	private WordServiceImpl wordService;

	private Word word(Integer id, String text, String description, int difficultyLevel, WordCategory category) {
		Word word = Word.builder()
			.word(text)
			.description(description)
			.difficultyLevel(difficultyLevel)
			.category(category)
			.build();
		// id는 SERIAL(자동증가)이라 빌더에 없다 — DB가 채워주는 값을 테스트에서 흉내낸다
		ReflectionTestUtils.setField(word, "id", id);
		return word;
	}

	@Test
	@DisplayName("카테고리 없이 조회하면 전체 단어를 id 오름차순, 명세 형식으로 내려준다")
	void getWords_success() {
		Page<Word> page = new PageImpl<>(
			List.of(word(1, "적금", "매달 정해진 돈을 모아 목표를 이루는 저축이에요.", 1, WordCategory.SAVINGS)),
			PageRequest.of(0, 20), 1);
		BDDMockito.given(wordRepository.findAllByOrderByIdAsc(PageRequest.of(0, 20))).willReturn(page);

		WordListResponse response = wordService.getWords(null, 0, 20);

		WordSummary summary = response.words().get(0);
		assertThat(summary.wordId()).isEqualTo(1);
		assertThat(summary.word()).isEqualTo("적금");
		assertThat(summary.difficultyLevel()).isEqualTo(1);
		assertThat(summary.category()).isEqualTo(WordCategory.SAVINGS);
		assertThat(response.page()).isEqualTo(0);
		assertThat(response.totalPages()).isEqualTo(1);
	}

	@Test
	@DisplayName("카테고리를 지정하면 해당 카테고리만 필터링해 조회한다")
	void getWords_filteredByCategory() {
		Page<Word> page = new PageImpl<>(
			List.of(word(2, "복리", "이자에 다시 이자가 붙는 계산 방식이에요.", 3, WordCategory.BANKING)),
			PageRequest.of(0, 20), 1);
		BDDMockito.given(wordRepository.findAllByCategoryOrderByIdAsc(WordCategory.BANKING, PageRequest.of(0, 20)))
			.willReturn(page);

		WordListResponse response = wordService.getWords(WordCategory.BANKING, 0, 20);

		assertThat(response.words()).hasSize(1);
		assertThat(response.words().get(0).category()).isEqualTo(WordCategory.BANKING);
	}

	@Test
	@DisplayName("단어가 하나도 없으면 빈 목록을 내려준다")
	void getWords_empty() {
		Page<Word> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
		BDDMockito.given(wordRepository.findAllByOrderByIdAsc(PageRequest.of(0, 20))).willReturn(page);

		WordListResponse response = wordService.getWords(null, 0, 20);

		assertThat(response.words()).isEmpty();
	}

	@Test
	@DisplayName("단어 상세를 명세 형식(definition 필드)으로 내려준다")
	void getWord_success() {
		BDDMockito.given(wordRepository.findById(1))
			.willReturn(Optional.of(word(1, "복리", "이자에 다시 이자가 붙는 계산 방식이에요.", 3, WordCategory.BANKING)));

		WordDetailResponse response = wordService.getWord(1);

		assertThat(response.wordId()).isEqualTo(1);
		assertThat(response.word()).isEqualTo("복리");
		assertThat(response.definition()).isEqualTo("이자에 다시 이자가 붙는 계산 방식이에요.");
		assertThat(response.difficultyLevel()).isEqualTo(3);
		assertThat(response.category()).isEqualTo(WordCategory.BANKING);
	}

	@Test
	@DisplayName("없는 단어를 상세 조회하면 WORD_NOT_FOUND 예외가 발생한다")
	void getWord_notFound() {
		BDDMockito.given(wordRepository.findById(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> wordService.getWord(99))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.WORD_NOT_FOUND);
	}

	@Test
	@DisplayName("키워드로 단어를 검색하면 명세 형식(definition 필드)으로 내려준다")
	void searchWords_success() {
		BDDMockito.given(wordRepository.findByWordContainingIgnoreCaseOrderByIdAsc("이자"))
			.willReturn(List.of(word(1, "이자", "돈을 맡기거나 빌려줘서 받는 보상", 1, WordCategory.BANKING)));

		WordSearchResponse response = wordService.searchWords("이자");

		assertThat(response.words()).hasSize(1);
		assertThat(response.words().get(0).word()).isEqualTo("이자");
		assertThat(response.words().get(0).definition()).isEqualTo("돈을 맡기거나 빌려줘서 받는 보상");
	}

	@Test
	@DisplayName("검색 결과가 없으면 빈 목록을 내려준다")
	void searchWords_empty() {
		BDDMockito.given(wordRepository.findByWordContainingIgnoreCaseOrderByIdAsc("없는단어")).willReturn(List.of());

		WordSearchResponse response = wordService.searchWords("없는단어");

		assertThat(response.words()).isEmpty();
	}

	@Test
	@DisplayName("keyword 없이 검색하면 INVALID_REQUEST 예외가 발생한다")
	void searchWords_blankKeyword() {
		assertThatThrownBy(() -> wordService.searchWords(" "))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_REQUEST);
	}
}
