package com.bana.dotori.word.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.word.entity.Word;
import com.bana.dotori.word.entity.WordCategory;

public interface WordRepository extends JpaRepository<Word, Integer> {

	Page<Word> findAllByOrderByIdAsc(Pageable pageable);

	Page<Word> findAllByCategoryOrderByIdAsc(WordCategory category, Pageable pageable);

	List<Word> findByWordContainingIgnoreCaseOrderByIdAsc(String keyword);
}
