package com.bana.dotori.family.dto;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

import jakarta.validation.constraints.Size;

/**
 * 가족 이름·온보딩 설정 부분 수정 요청. 안 보낸(null) 필드는 그대로 둔다
 * (회원 정보 수정 UserUpdateRequest 와 같은 방식 — 이름은 앞뒤 공백을 털어내고
 * 빈 문자열이면 {@code @Size} 가 걸러낸다).
 *
 * <p>금액 두 개는 원(₩) 단위다. 금액 검증(최소 1000원·1000원 단위)은 생성과 마찬가지로
 * 서비스의 toPoints 가 전담한다 — 에러 형태를 한 곳에서 내기 위해 DTO 에는 두지 않는다.
 */
public record FamilyUpdateRequest(
	@Size(min = 1, max = 100) String name,
	Integer weeklyAllowance,
	Integer weeklyRewardLimit,
	FinancialKnowledgeLevel financialKnowledgeLevel
) {

	public FamilyUpdateRequest {
		if (name != null) {
			name = name.trim();
		}
	}
}
