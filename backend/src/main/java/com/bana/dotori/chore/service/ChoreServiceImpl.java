package com.bana.dotori.chore.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.mapper.ChoreMapper;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChoreServiceImpl implements ChoreService {

	private final ChoreRepository choreRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final UserRepository userRepository;
	private final ChoreMapper choreMapper;

	@Override
	@Transactional
	public ChoreDetailResponse createChore(Integer userId, ChoreCreateRequest request) {
		requireParent(userId);
		FamilyMember child = findMyChild(userId, request.childId());

		Chore chore = choreRepository.save(Chore.builder()
			.familyId(child.getFamily().getId())
			.parentId(userId)
			.childId(child.getUser().getId())
			.title(request.title())
			.description(request.description())
			.rewardPoints(request.rewardPoints())
			.repeatType(request.repeatType())
			.startDate(request.startDate())
			.endDate(request.endDate())
			.build());

		return choreMapper.toDetailResponse(chore);
	}

	private void requireParent(Integer userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	/**
	 * 나와 연결된 자녀 중 그 아이를 찾는다. familyId 는 여기서 얻는다.
	 *
	 * <p>요청 본문의 childId 만 믿으면 남의 가족 아이에게 집안일을 심을 수 있다.
	 * 기존 자녀 조회 쿼리를 그대로 쓰는 이유는 그쪽이 소프트 삭제를 세 군데
	 * (users·family_members·families) 모두 보기 때문이다. 조건을 여기서 다시 쓰면
	 * 하나를 빼먹어도 결과가 그럴듯하게 나와 눈치채기 어렵다.
	 */
	private FamilyMember findMyChild(Integer parentId, Integer childId) {
		return familyMemberRepository.findConnectedChildren(parentId, UserRole.CHILD).stream()
			.filter(member -> member.getUser().getId().equals(childId))
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
	}
}
