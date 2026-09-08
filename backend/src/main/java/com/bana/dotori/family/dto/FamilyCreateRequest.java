package com.bana.dotori.family.dto;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record FamilyCreateRequest(
	@NotBlank @Size(max = 100) String name,
	/* 온보딩 1 — 아이가 매주 받는 기본 용돈(원) */
	@NotNull @PositiveOrZero Integer weeklyAllowance,
	/* 온보딩 2 — 도토리 활동으로 한 주 동안 받을 수 있는 추가 보상 한도(원) */
	@NotNull @PositiveOrZero Integer weeklyRewardLimit,
	/* 온보딩 3 — 아이의 금융 지식 수준 */
	@NotNull FinancialKnowledgeLevel financialKnowledgeLevel
) {
}
