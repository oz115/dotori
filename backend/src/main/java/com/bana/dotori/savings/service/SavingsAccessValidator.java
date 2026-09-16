package com.bana.dotori.savings.service;

import org.springframework.stereotype.Component;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

/**
 * 예적금 API 경로(`/families/{familyId}/children/{childId}/savings...`)의 접근 검증.
 * {@code ChoreAccessValidator} 와 같은 이유로 도메인마다 따로 둔다 — 규칙을 바꿀 일이 생기면
 * 이 클래스만 고친다.
 */
@Component
@RequiredArgsConstructor
public class SavingsAccessValidator {

	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;

	/**
	 * 경로의 familyId·childId 를 검증한다.
	 *
	 * <ol>
	 *   <li>살아 있는 가족이고 호출자가 그 구성원인가 — 아니면 404 (존재 여부를 숨긴다).</li>
	 *   <li>childId 가 그 가족의 살아 있는 자녀인가 — 아니면 404.</li>
	 *   <li>호출자가 아이라면 자기 자신인가 — 형제 것을 보려 하면 403.</li>
	 * </ol>
	 */
	public void validate(Integer userId, Integer familyId, Integer childId) {
		familyRepository.findByIdAndDeletedAtIsNull(familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		if (!familyMemberRepository.existsActiveMember(familyId, userId)) {
			throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
		}

		FamilyMember childMember = familyMemberRepository.findWithUserByFamilyIdAndUserId(familyId, childId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		if (childMember.getUser().getRole() != UserRole.CHILD) {
			throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
		}

		FamilyMember caller = familyMemberRepository.findWithUserByFamilyIdAndUserId(familyId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		if (caller.getUser().getRole() == UserRole.CHILD && !userId.equals(childId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}
}
