package com.bana.dotori.family.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

/**
 * 가족 설정 수정 응답. 금액 두 개는 요청과 대칭이 되도록 원(₩)으로 되돌려 준다
 * (저장은 포인트지만, 이 API 의 입출력 단위는 부모가 화면에서 보는 원이다).
 */
public record FamilyUpdateResponse(
	Integer familyId,
	String name,
	Integer weeklyAllowance,
	Integer weeklyRewardLimit,
	FinancialKnowledgeLevel financialKnowledgeLevel,
	OffsetDateTime updatedAt
) {
}
