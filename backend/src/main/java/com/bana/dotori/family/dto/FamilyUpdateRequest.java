package com.bana.dotori.family.dto;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 가족 이름·온보딩 설정 부분 수정 요청. 안 보낸(null) 필드는 그대로 둔다
 * (회원 정보 수정 UserUpdateRequest 와 같은 방식 — 이름은 앞뒤 공백을 털어내고
 * 빈 문자열이면 {@code @Size} 가 걸러낸다).
 *
 * <p>금액 두 개는 원(₩) 단위다. 최소 1000원·1000원 단위 검증과 포인트 환산(10원=1포인트)은
 * 가족 생성과 같은 정책이라 서비스에서 한다.
 */
public record FamilyUpdateRequest(
	@Size(min = 1, max = 100) String name,
	@Positive Integer weeklyAllowance,
	@Positive Integer weeklyRewardLimit,
	FinancialKnowledgeLevel financialKnowledgeLevel
) {

	public FamilyUpdateRequest {
		if (name != null) {
			name = name.trim();
		}
	}
}
