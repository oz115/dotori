package com.bana.dotori.family.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

/**
 * 가족 조회 응답. 온보딩 설정 3종을 함께 내려 설정 수정 화면이 현재 값을 미리 채울 수 있게 한다.
 * 금액 두 개는 화면 단위인 원(₩)이다 — 저장(포인트)에서 응답으로 나갈 때 환산한다.
 */
public record FamilyResponse(
	Integer familyId,
	String name,
	Integer createdBy,
	int memberCount,
	Integer weeklyAllowance,
	Integer weeklyRewardLimit,
	FinancialKnowledgeLevel financialKnowledgeLevel,
	OffsetDateTime createdAt
) {
}
