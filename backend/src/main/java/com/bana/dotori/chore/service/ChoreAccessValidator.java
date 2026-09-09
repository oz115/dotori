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
 * 완료 요청 계열(CHORE-08~12)이 같은 검증을 여러 번 하므로 한곳에 모았다.
 *
 * <p>{@code ChoreServiceImpl} 에도 같은 로직이 private 메서드로 남아 있다. 그쪽까지 옮기면
 * 기존 서비스 테스트가 검증하던 404·403 케이스가 모킹 뒤로 숨어 버려 이번 범위에서는 두었다.
 * 인가 규칙이 두 군데 있는 상태라 한쪽만 고치는 일이 없도록 조심해야 한다.
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
