package com.bana.dotori.quiz.service;

import org.springframework.stereotype.Component;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 퀴즈 API 경로(`/families/{familyId}/children/{childId}/quizzes/...`)의 접근 검증.
 *
 * <p>{@code ChoreAccessValidator}와 같은 이유로 도메인마다 따로 둔다 — 규칙이 한 곳에만
 * 있어야 절반만 고쳐진 상태로 남는 일이 없다.
 */
@Component
@RequiredArgsConstructor
public class QuizAccessValidator {

	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final UserRepository userRepository;

	/**
	 * 경로의 familyId·childId를 검증한다. {@code ChoreAccessValidator#validate}와 같은 규칙이다.
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

		if (findUser(userId).getRole() == UserRole.CHILD && !userId.equals(childId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	/**
	 * 퀴즈 조회·제출은 아이 본인만 할 수 있다 (QUIZ-02·03, "사용자: 아이"). 부모는
	 * {@link #validate}는 통과하므로 여기서 한 번 더 막는다.
	 */
	public void requireChild(Integer userId) {
		if (findUser(userId).getRole() != UserRole.CHILD) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	private User findUser(Integer userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}
}
