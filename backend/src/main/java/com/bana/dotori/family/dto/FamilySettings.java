package com.bana.dotori.family.dto;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

/**
 * 온보딩에서 정한 가족 설정값. 퀴즈·예적금·투자·집안일 도메인이 소비한다.
 *
 * <p>금액 두 개는 <b>포인트 단위</b>다 (부모가 입력한 원을 저장 시점에 10원=1포인트로 환산).
 * 소비처는 포인트 그대로 계산하면 되고, 화면 표시용 원 환산은 프론트 몫이다.
 */
public record FamilySettings(
	Integer weeklyAllowance,
	Integer weeklyRewardLimit,
	FinancialKnowledgeLevel financialKnowledgeLevel
) {
}
