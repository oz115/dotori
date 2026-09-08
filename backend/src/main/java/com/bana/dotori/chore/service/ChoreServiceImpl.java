package com.bana.dotori.chore.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreListResponse;
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

	@Override
	@Transactional(readOnly = true)
	public ChoreListResponse getChores(Integer userId, Integer childId) {
		User user = findUser(userId);
		Integer familyId = findMyFamilyId(userId);

		if (user.getRole() == UserRole.CHILD) {
			// 아이가 넘긴 childId 는 쓰지 않는다. 남의 것을 넣어도 자기 몫만 나와야 한다.
			return choreMapper.toListResponse(findByChild(familyId, userId));
		}
		List<Chore> chores = childId != null
			? findByChild(familyId, childId)
			: choreRepository.findAllByFamilyIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(familyId);
		return choreMapper.toListResponse(chores);
	}

	@Override
	@Transactional(readOnly = true)
	public ChoreDetailResponse getChore(Integer userId, Integer choreId) {
		User user = findUser(userId);
		Integer familyId = findMyFamilyId(userId);

		Chore chore = choreRepository.findByIdAndFamilyId(choreId, familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_NOT_FOUND));

		// 아이는 형제자매에게 배정된 집안일을 볼 이유가 없다.
		// 403 이 아니라 404 인 이유는 403 이면 그 choreId 의 존재가 새어 나가기 때문이다.
		if (user.getRole() == UserRole.CHILD && !chore.getChildId().equals(userId)) {
			throw new BusinessException(ErrorCode.CHORE_NOT_FOUND);
		}
		return choreMapper.toDetailResponse(chore);
	}

	private List<Chore> findByChild(Integer familyId, Integer childId) {
		return choreRepository.findAllByFamilyIdAndChildIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(
			familyId, childId);
	}

	private User findUser(Integer userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}

	private void requireParent(Integer userId) {
		if (findUser(userId).getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	/**
	 * 경로에 familyId 가 없으므로 인증 주체에서 가족을 찾는다.
	 *
	 * <p>가족에 속하지 않은 회원은 집안일을 볼 것도 없다. 빈 목록 대신 404 를 주는 이유는
	 * "가족이 없다"와 "집안일이 없다"를 클라이언트가 구분해야 하기 때문이다.
	 */
	private Integer findMyFamilyId(Integer userId) {
		return familyMemberRepository.findActiveFamilyIdByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
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
