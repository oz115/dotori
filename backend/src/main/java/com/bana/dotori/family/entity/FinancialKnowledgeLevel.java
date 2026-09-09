package com.bana.dotori.family.entity;

/**
 * 온보딩에서 부모가 고르는 아이의 금융 지식 수준.
 * 퀴즈 난이도·설명 문구 조정에 쓴다.
 */
public enum FinancialKnowledgeLevel {
	/** 처음이에요 — 용돈과 저축의 개념부터 */
	BEGINNER,
	/** 알아가고 있어요 — 예/적금과 이자를 조금 안다 */
	INTERMEDIATE,
	/** 익숙해요 — 투자의 수익·위험 관계를 이해한다 */
	ADVANCED
}
