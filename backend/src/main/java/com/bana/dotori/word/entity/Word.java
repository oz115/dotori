package com.bana.dotori.word.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경제 용어 사전 (단어장). 퀴즈 생성의 재료이기도 하지만, 이 엔티티는 단어장 API(조회 전용)가
 * 쓰는 컬럼만 매핑한다 — 가족·자녀 구분이 없는 전역 테이블이라 family_id 같은 컬럼이 없다.
 */
@Entity
@Table(name = "words")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Word {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "word_id")
	private Integer id;

	@Column(name = "word", length = 100, nullable = false)
	private String word;

	@Column(name = "description", length = 255, nullable = false)
	private String description;

	// 아이 눈높이로 풀어 쓴 설명. 용어사전을 나중에 받아 채운 컬럼이라 아직 비어 있는 단어가 있다.
	@Column(name = "easy_description", length = 255)
	private String easyDescription;

	@Column(name = "difficulty_level", nullable = false)
	private Integer difficultyLevel;

	// ERD 상 NULL 허용 — 분류가 정해지지 않은 단어가 있을 수 있다
	@Enumerated(EnumType.STRING)
	@Column(name = "category")
	private WordCategory category;

	@Builder
	private Word(String word, String description, String easyDescription, Integer difficultyLevel,
			WordCategory category) {
		this.word = word;
		this.description = description;
		this.easyDescription = easyDescription;
		this.difficultyLevel = difficultyLevel;
		this.category = category;
	}
}
