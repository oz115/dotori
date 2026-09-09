package com.bana.dotori.chore.entity;

/**
 * 할 일 난이도.
 *
 * <p>DB의 task_difficulty enum 타입과 값이 같아야 한다.
 * AI 가 제목·설명을 보고 판정하며, 주간 기본 용돈(families.weekly_allowance) 대비
 * 보상 가중치(chores.value)를 매기는 근거가 된다.
 *
 * <p>타입명이 chores_ 로 시작하지 않는 이유는 집안일 외의 할 일에도 쓸 수 있게
 * DDL 에서 공용으로 잡았기 때문이다.
 */
public enum ChoreDifficulty {
	EASY,
	NORMAL,
	HARD
}
