package com.bana.dotori.word.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.word.entity.Word;
import com.bana.dotori.word.entity.WordCategory;

public interface WordRepository extends JpaRepository<Word, Integer> {

	// 목록 화면이 초성(가나다) 그룹으로 보여주므로 페이지 순서도 가나다순이어야
	// 무한 스크롤로 페이지를 이어 붙일 때 그룹 사이에 단어가 끼어들지 않는다
	Page<Word> findAllByOrderByWordAscIdAsc(Pageable pageable);

	Page<Word> findAllByCategoryOrderByWordAscIdAsc(WordCategory category, Pageable pageable);

	List<Word> findByWordContainingIgnoreCaseOrderByWordAscIdAsc(String keyword);
}
