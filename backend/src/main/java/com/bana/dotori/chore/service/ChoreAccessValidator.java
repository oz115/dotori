package com.bana.dotori.chore.service;

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
 * 집안일 API 경로(`/families/{familyId}/children/{childId}/...`)의 접근 검증.
 *
 * <p>경로에 들어온 familyId·childId 를 그대로 믿으면 남의 가족 데이터를 건드릴 수 있다.
 * 집안일 API 전부(CHORE-01~13)가 같은 검증을 하므로 한곳에 모았다.
 *
 * <p><b>집안일 인가 규칙은 이 클래스에만 있다.</b> 한동안 {@code ChoreServiceImpl} 안에도
 * 같은 로직이 private 으로 복제돼 있었는데, 그 상태에서는 규칙을 한쪽만 고치면 나머지 절반이
 * 옛 규칙으로 돌고 양쪽 테스트가 모두 초록이라 아무도 눈치채지 못한다. 규칙을 바꿀 일이
 * 생기면 여기만 고치고, 다른 곳에 같은 검사를 다시 쓰지 않는다.
 *
 * <p>규칙이 맞는지는 {@code ChoreAccessValidatorTest} 가 검증한다. 각 서비스 테스트는
 * 이 검증기를 모킹하므로 "거치는가"만 본다.
 */
@Component
@RequiredArgsConstructor
public class ChoreAccessValidator {

	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final UserRepository userRepository;

	/**
	 * 경로의 familyId·childId 를 검증한다. 세 가지를 본다.
	 *
	 * <ol>
	 *   <li>살아 있는 가족이고 호출자가 그 구성원인가 — 아니면 404.
	 *       403 을 주면 그 familyId 가 실재한다는 사실이 새어 나간다.</li>
	 *   <li>childId 가 그 가족의 살아 있는 자녀인가 — 아니면 404.</li>
	 *   <li>호출자가 아이라면 자기 자신인가 — 형제 것을 보려 하면 403.
	 *       형제의 존재는 이미 아는 사실이라 숨길 것이 없고, 권한 문제임을 알려 주는 편이 낫다.</li>
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

		if (findUser(userId).getRole() == UserRole.CHILD && !userId.equals(childId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	/**
	 * 아이만 할 수 있는 동작(CHORE-11·12)을 막는다.
	 *
	 * <p>{@link #validate} 는 "아이라면 자기 것인가"만 보므로 부모도 통과한다.
	 * 완료 인증은 아이가 올리는 것이라 여기서 한 번 더 좁힌다.
	 */
	public void requireChild(Integer userId) {
		if (findUser(userId).getRole() != UserRole.CHILD) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	/**
	 * 부모만 할 수 있는 동작(CHORE-08·09·10)을 막는다.
	 *
	 * <p>{@link #validate} 만으로는 아이가 자기 대기함을 들여다볼 수 있다. 승인·거절 화면은
	 * 부모 것이라 여기서 한 번 더 좁힌다.
	 */
	public void requireParent(Integer userId) {
		if (findUser(userId).getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	public User findUser(Integer userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}
}
