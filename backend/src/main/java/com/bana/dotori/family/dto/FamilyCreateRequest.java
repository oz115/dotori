package com.bana.dotori.family.dto;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 금액 두 개는 부모가 화면에서 입력하는 원(₩) 단위다. 서버가 10원=1포인트로 환산해
 * 포인트로 저장한다. 금액 검증(최소 1000원·1000원 단위)은 여기에 두지 않고 서비스의
 * toPoints 가 전담한다 — DTO 와 서비스로 나뉘면 같은 실수(금액 오류)가 값에 따라
 * 서로 다른 에러 형태로 나가 프론트의 필드 매핑이 갈라진다.
 *
 * <p>name 은 수정 요청(FamilyUpdateRequest)과 같은 규칙으로 앞뒤 공백을 털어낸다 —
 * 생성과 수정의 정규화가 다르면 "같은 문자열로 PATCH 했는데 값이 바뀌는" 비대칭이 생긴다.
 */
public record FamilyCreateRequest(
	@NotBlank @Size(max = 100) String name,
	/* 온보딩 1 — 아이가 매주 받는 기본 용돈(원) */
	@NotNull Integer weeklyAllowance,
	/* 온보딩 2 — 도토리 활동으로 한 주 동안 받을 수 있는 추가 보상 한도(원) */
	@NotNull Integer weeklyRewardLimit,
	/* 온보딩 3 — 아이의 금융 지식 수준 */
	@NotNull FinancialKnowledgeLevel financialKnowledgeLevel
) {

	public FamilyCreateRequest {
		if (name != null) {
			name = name.trim();
		}
	}
}
