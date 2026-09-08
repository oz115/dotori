package com.bana.dotori.family.dto;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

/**
 * 온보딩에서 정한 가족 설정값. 퀴즈·예적금·투자·집안일 도메인이 소비한다.
 */
public record FamilySettings(
	Integer weeklyAllowance,
	Integer weeklyRewardLimit,
	FinancialKnowledgeLevel financialKnowledgeLevel
) {
}
