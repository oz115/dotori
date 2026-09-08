package com.bana.dotori.family.dto;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 금액 두 개는 부모가 화면에서 입력하는 원(₩) 단위다. 서버가 10원=1포인트로 환산해
 * 포인트로 저장하며, 최소 1000원·1000원 단위 검증은 정책이라 서비스에서 한다
 * (포인트 충전 PointChargeRequest 와 같은 방식).
 */
public record FamilyCreateRequest(
	@NotBlank @Size(max = 100) String name,
	/* 온보딩 1 — 아이가 매주 받는 기본 용돈(원) */
	@NotNull @Positive Integer weeklyAllowance,
	/* 온보딩 2 — 도토리 활동으로 한 주 동안 받을 수 있는 추가 보상 한도(원) */
	@NotNull @Positive Integer weeklyRewardLimit,
	/* 온보딩 3 — 아이의 금융 지식 수준 */
	@NotNull FinancialKnowledgeLevel financialKnowledgeLevel
) {
}
