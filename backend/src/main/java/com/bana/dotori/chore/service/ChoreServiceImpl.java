package com.bana.dotori.chore.service;

import java.time.LocalDate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreListResponse;
import com.bana.dotori.chore.dto.ChoreUpdateRequest;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.event.ChoreCreatedEvent;
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
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public ChoreDetailResponse createChore(Integer userId, Integer familyId, Integer childId,
			ChoreCreateRequest request) {
		validateAccess(userId, familyId, childId);
		if (findUser(userId).getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		// 반복이면 오늘부터 주기 끝까지, 일회성이면 부모가 넣은 기간을 쓴다.
		// 반복에서 요청의 날짜를 무시하는 이유는 크론이 주기마다 기간을 다시 계산해 밀기 때문이다.
		// 부모가 넣은 날짜를 그대로 두면 첫 주기만 다르게 굴러간다.
		boolean repeating = request.repeatType().isRepeating();
		LocalDate startDate = repeating ? LocalDate.now() : request.startDate();
		LocalDate endDate = repeating ? request.repeatType().endDateFrom(startDate) : request.endDate();

		Chore chore = choreRepository.save(Chore.builder()
			.familyId(familyId)
			.parentId(userId)
			.childId(childId)
			.title(request.title())
			.description(request.description())
			.difficulty(request.difficulty())
			.repeatType(request.repeatType())
			.startDate(startDate)
			.endDate(endDate)
			.build());

		// value·rewardPoints 는 AI 서버가 비동기로 채운다 — 커밋 이후에만 발행되도록 이벤트로 넘긴다.
		eventPublisher.publishEvent(new ChoreCreatedEvent(chore.getId(), familyId, childId,
			chore.getTitle(), chore.getDescription(), chore.getDifficulty(), chore.getRepeatType()));

		return choreMapper.toDetailResponse(chore);
	}

	@Override
	@Transactional(readOnly = true)
	public ChoreListResponse getChores(Integer userId, Integer familyId, Integer childId) {
		validateAccess(userId, familyId, childId);
		return choreMapper.toListResponse(
			choreRepository.findAllByFamilyIdAndChildIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(familyId, childId));
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

		chore.update(request.title(), request.description(), request.difficulty(), request.rewardPoints());
		applyPeriod(chore, request);

		return choreMapper.toDetailResponse(chore);
	}

	@Override
	@Transactional
	public void deleteChore(Integer userId, Integer familyId, Integer childId, Integer choreId) {
		findForParent(userId, familyId, childId, choreId).softDelete();
	}

	/**
	 * 수정 요청의 주기·기간을 반영한다 (CHORE-05).
	 *
	 * <p>주기를 바꾸면 기간을 다시 정한다. 주기만 바꾸고 기간을 그대로 두면 예전 주기의
	 * 날짜가 남아, 일회성(9/10~9/20)을 매일로 바꿨을 때 "매일 반복인데 9/20 에 끝나는"
	 * 상태가 된다. 크론은 end_date 가 지난 것만 밀기 때문에 그날이 와야 정상으로 돌아온다.
	 *
	 * <ul>
	 *   <li>반복으로 바꿈 → 오늘 기준으로 계산한다. 요청의 날짜는 쓰지 않는다.</li>
	 *   <li>일회성으로 바꿈 → 날짜를 함께 받아야 한다. 종료일을 계산할 근거가 없다.</li>
	 *   <li>주기를 그대로 둠
	 *     <ul>
	 *       <li>반복이면 손대지 않는다. 기간은 크론이 관리한다.</li>
	 *       <li>일회성이면 보낸 날짜만 반영한다.</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 */
	private void applyPeriod(Chore chore, ChoreUpdateRequest request) {
		ChoreRepeatType target = request.repeatType() != null ? request.repeatType() : chore.getRepeatType();
		boolean typeChanged = request.repeatType() != null && request.repeatType() != chore.getRepeatType();

		if (target.isRepeating()) {
			if (typeChanged) {
				LocalDate today = LocalDate.now();
				chore.changePeriod(target, today, target.endDateFrom(today));
			}
			return;
		}

		if (typeChanged) {
			if (request.startDate() == null || request.endDate() == null) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST,
					"반복하지 않는 할 일로 바꿀 때는 시작일과 종료일이 필요합니다");
			}
			chore.changePeriod(ChoreRepeatType.ONCE, request.startDate(), request.endDate());
			return;
		}

		if (request.startDate() != null || request.endDate() != null) {
			// 한쪽만 보낼 수 있다. 보내지 않은 쪽은 지금 값을 그대로 쓴다.
			chore.changePeriod(ChoreRepeatType.ONCE,
				request.startDate() != null ? request.startDate() : chore.getStartDate(),
				request.endDate() != null ? request.endDate() : chore.getEndDate());
		}
	}

	/**
	 * 부모만 할 수 있는 변경(CHORE-05·06)의 공통 진입점.
	 *
	 * <p>이미 지운 집안일도 찾는다. 이미 지운 것을 고치는 일은 드물지만, 걸려 있는 완료 요청 때문에
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
