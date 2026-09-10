package com.bana.dotori.chore.service;

import java.time.LocalDate;
import java.time.ZoneId;

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
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChoreServiceImpl implements ChoreService {

	/**
	 * 생성·수정이 보는 "오늘"을 KST 로 고정한다.
	 *
	 * <p>{@code LocalDate.now()} 는 시스템 기본 타임존을 쓴다. 지금 배포 환경은
	 * 컨테이너에 {@code TZ=Asia/Seoul} 이 들어가 있어 그대로 써도 KST 가 나오지만,
	 * 그 설정은 서버의 배포 파일에만 있고 이 저장소에는 흔적이 없다
	 * (backend/Dockerfile 에는 TZ 가 없어 이미지만 보면 UTC 다).
	 *
	 * <p>그 설정이 빠진 환경(CI, 다른 배포 대상, 이미지 재사용)에서는 UTC 가 되고,
	 * 그러면 한국 시간 새벽에 등록한 반복 할 일이 어제 날짜로 저장된다. 크론은
	 * KST 를 코드에 박아 두었으므로(SchedulingConfig 의 Clock) 그날 자정에
	 * "기간이 끝난 할 일"로 보고 곧바로 다음 주기로 밀어 버리고, 부모가 방금 만든
	 * 할 일이 이미 지나간 기간을 가리키게 된다.
	 *
	 * <p>크론만 명시하고 여기는 환경에 맡기면 둘이 갈릴 수 있어 같은 층위로 맞춘다.
	 * 상점 구매(StorePurchaseService)도 같은 방식이다.
	 */
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final ChoreRepository choreRepository;
	private final ChoreAccessValidator accessValidator;
	private final ChoreMapper choreMapper;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public ChoreDetailResponse createChore(Integer userId, Integer familyId, Integer childId,
			ChoreCreateRequest request) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireParent(userId);

		// start_date~end_date 는 반복과 일회성에서 뜻이 다르다.
		//   반복   — 지금 진행 중인 "주기 한 칸"(창). 칸이 끝나면 크론이 다음 칸으로 민다.
		//   일회성 — 이 할 일의 전체 수명. 크론이 건드리지 않아 부모가 정한 값이 끝까지 남는다.
		//
		// 그래서 반복은 요청의 날짜를 읽지 않고 주기로 계산한다. 부모가 넣은 임의의 기간을
		// 쓰면 그 칸의 길이가 주기와 맞지 않게 되고, 주기당 한 번만 지급하는 규칙
		// (S15P21A505-126)이 그 칸을 기준으로 판정하므로 보상 횟수가 틀어진다.
		// 매일 반복에 9/1~12/31 이 들어온 경우: 크론은 end_date 가 지난 것만 밀기 때문에
		// 12/31 까지 칸이 하나로 남고, 그 4개월 동안 보상이 단 한 번 나간다.
		boolean repeating = request.repeatType().isRepeating();
		LocalDate startDate = repeating ? LocalDate.now(KST) : request.startDate();
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
		accessValidator.validate(userId, familyId, childId);
		return choreMapper.toListResponse(
			choreRepository.findAllByFamilyIdAndChildIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(familyId, childId));
	}

	@Override
	@Transactional(readOnly = true)
	public ChoreDetailResponse getChore(Integer userId, Integer familyId, Integer childId, Integer choreId) {
		accessValidator.validate(userId, familyId, childId);

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
				LocalDate today = LocalDate.now(KST);
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
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireParent(userId);

		Chore chore = choreRepository.findByIdAndFamilyId(choreId, familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_NOT_FOUND));
		if (!chore.getChildId().equals(childId)) {
			throw new BusinessException(ErrorCode.CHORE_NOT_FOUND);
		}
		return chore;
	}
}
