package com.bana.dotori.chore.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreListResponse;
import com.bana.dotori.chore.dto.ChoreUpdateRequest;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.mapper.ChoreMapper;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
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
	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final UserRepository userRepository;
	private final ChoreMapper choreMapper;

	@Override
	@Transactional
	public ChoreDetailResponse createChore(Integer userId, Integer familyId, Integer childId,
			ChoreCreateRequest request) {
		validateAccess(userId, familyId, childId);
		if (findUser(userId).getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		Chore chore = choreRepository.save(Chore.builder()
			.familyId(familyId)
			.parentId(userId)
			.childId(childId)
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
	public ChoreListResponse getChores(Integer userId, Integer familyId, Integer childId) {
		validateAccess(userId, familyId, childId);
		return choreMapper.toListResponse(
			choreRepository.findAllByFamilyIdAndChildIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(familyId, childId));
	}

	@Override
	@Transactional(readOnly = true)
	public ChoreDetailResponse getChore(Integer userId, Integer familyId, Integer childId, Integer choreId) {
		validateAccess(userId, familyId, childId);

		Chore chore = choreRepository.findByIdAndFamilyId(choreId, familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_NOT_FOUND));

		// 경로가 지목한 아이의 집안일이 아니면 없는 것으로 본다.
		// 같은 가족이라도 경로와 실제가 어긋나면 잘못된 요청이고,
		// 404 로 돌려줘야 그 choreId 가 누구 것인지 짚어 보는 것을 막을 수 있다.
		if (!chore.getChildId().equals(childId)) {
			throw new BusinessException(ErrorCode.CHORE_NOT_FOUND);
		}
		return choreMapper.toDetailResponse(chore);
	}

	@Override
	@Transactional
	public ChoreDetailResponse updateChore(Integer userId, Integer familyId, Integer childId, Integer choreId,
			ChoreUpdateRequest request) {
		Chore chore = findForParent(userId, familyId, childId, choreId);

		chore.update(request.title(), request.description(), request.rewardPoints(), request.repeatType(),
			request.startDate(), request.endDate());

		return choreMapper.toDetailResponse(chore);
	}

	@Override
	@Transactional
	public void deactivateChore(Integer userId, Integer familyId, Integer childId, Integer choreId) {
		findForParent(userId, familyId, childId, choreId).deactivate();
	}

	/**
	 * 부모만 할 수 있는 변경(CHORE-05·06)의 공통 진입점.
	 *
	 * <p>비활성 집안일도 찾는다. 이미 지운 것을 고치는 일은 드물지만, 걸려 있는 완료 요청 때문에
	 * 보상 금액을 바로잡아야 하는 경우가 있다.
	 */
	private Chore findForParent(Integer userId, Integer familyId, Integer childId, Integer choreId) {
		validateAccess(userId, familyId, childId);
		if (findUser(userId).getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		Chore chore = choreRepository.findByIdAndFamilyId(choreId, familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_NOT_FOUND));
		if (!chore.getChildId().equals(childId)) {
			throw new BusinessException(ErrorCode.CHORE_NOT_FOUND);
		}
		return chore;
	}

	/**
	 * 경로의 familyId·childId 를 검증한다. 세 가지를 본다.
	 *
	 * <ol>
	 *   <li>살아 있는 가족이고 호출자가 그 구성원인가 — 아니면 404.
	 *       403 을 주면 그 familyId 가 실재한다는 사실이 새어 나간다 (상점 API 와 같은 기준).</li>
	 *   <li>childId 가 그 가족의 살아 있는 자녀인가 — 아니면 404.</li>
	 *   <li>호출자가 아이라면 자기 자신인가 — 형제 것을 보려 하면 403.
	 *       형제의 존재는 이미 아는 사실이라 여기서는 숨길 것이 없고, 권한 문제임을 알려 주는 편이 낫다.</li>
	 * </ol>
	 */
	private void validateAccess(Integer userId, Integer familyId, Integer childId) {
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

	private User findUser(Integer userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}
}
