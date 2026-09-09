package com.bana.dotori.chore.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.chore.dto.ChoreCompletionCreateRequest;
import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreCompletionRequest;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;
import com.bana.dotori.chore.mapper.ChoreCompletionMapper;
import com.bana.dotori.chore.repository.ChoreCompletionRequestRepository;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChoreCompletionServiceImpl implements ChoreCompletionService {

	/**
	 * "오늘"을 KST 로 고정한다. 이유는 {@code ChoreServiceImpl.KST} 주석과 같다 —
	 * 시스템 기본 타임존을 따라가면 배포 환경에 따라 기간 판정이 하루씩 어긋난다.
	 */
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final ChoreRepository choreRepository;
	private final ChoreCompletionRequestRepository completionRequestRepository;
	private final ChoreAccessValidator accessValidator;
	private final ChoreCompletionMapper completionMapper;

	/**
	 * 아이가 완료를 인증한다 (CHORE-11).
	 *
	 * <p>막는 경우가 넷이다. 순서는 "요청이 성립하지 않는 이유"부터 "지금은 안 되는 이유" 순이다.
	 *
	 * <ol>
	 *   <li>지워진 할 일 — 새 요청은 받지 않는다. 이미 올라온 요청의 처리는 계속 된다.</li>
	 *   <li>기간 밖 — 오늘이 이번 주기 안이어야 한다.</li>
	 *   <li>보상 미산정 — AI 가 아직 값을 못 채웠으면 승인해도 줄 금액이 없다.
	 *       상점의 {@code PRICE_NOT_EVALUATED} 와 같은 자리다.</li>
	 *   <li>이미 대기 중이거나 이번 주기에 이미 받았음 — 둘 다 409.</li>
	 * </ol>
	 */
	@Override
	@Transactional
	public ChoreCompletionResponse requestCompletion(Integer userId, Integer familyId, Integer childId,
			Integer choreId, ChoreCompletionCreateRequest request) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireChild(userId);

		Chore chore = findChore(familyId, childId, choreId);

		if (chore.isDeleted()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "지워진 할 일에는 완료 요청을 보낼 수 없습니다");
		}
		if (!chore.isInPeriod(LocalDate.now(KST))) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST,
					"수행 기간이 아닙니다. choreId=" + choreId + ", 기간=" + chore.getStartDate() + "~" + chore.getEndDate());
		}
		if (chore.getRewardPoints() == null) {
			throw new BusinessException(ErrorCode.REWARD_NOT_EVALUATED);
		}
		if (completionRequestRepository.existsByChoreIdAndStatus(choreId, ChoreCompletionStatus.PENDING)) {
			throw new BusinessException(ErrorCode.PENDING_REQUEST_EXISTS);
		}

		// 이번 주기에 이미 지급됐으면 요청을 받아 봐야 승인 단계에서 막힌다.
		// 부모 대기함에 처리할 수 없는 항목을 쌓지 않으려고 여기서 끊는다.
		if (completionRequestRepository.hasApprovedSince(choreId, chore.currentPeriodStartedAt())) {
			throw new BusinessException(ErrorCode.ALREADY_REWARDED);
		}

		ChoreCompletionRequest saved = completionRequestRepository.save(ChoreCompletionRequest.builder()
			.choreId(choreId)
			.childId(childId)
			.photoUrl(request.photoUrl())
			.description(request.description())
			.build());

		return completionMapper.toResponse(saved);
	}

	/**
	 * 아이가 자기 요청을 물린다 (CHORE-12).
	 *
	 * <p>행을 지우지 않고 CANCELLED 로 남긴다. 취소 이력이 남아야 부모가 "왜 사라졌지" 를
	 * 되짚을 수 있고, 주기당 지급 판정은 SUCCEED 만 보므로 남아 있어도 재요청을 막지 않는다.
	 */
	@Override
	@Transactional
	public void cancelCompletion(Integer userId, Integer familyId, Integer childId, Integer requestId) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireChild(userId);

		ChoreCompletionRequest completionRequest = completionRequestRepository.findById(requestId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND));

		// 남의 요청이면 403 이다. 여기서는 404 로 숨기지 않는다 — 명세가 403 이고,
		// 요청 ID 는 아이가 자기 화면에서 이미 받아 본 값이라 존재를 감출 실익이 없다.
		if (!completionRequest.getChildId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		// 상태 검사는 엔티티가 한다. 이미 처리된 요청이면 INVALID_STATUS 로 409 가 나간다.
		completionRequest.cancel(OffsetDateTime.now(KST));
	}

	/**
	 * 경로가 지목한 아이의 할 일을 집는다.
	 *
	 * <p>가족으로 먼저 좁히고 아이가 어긋나면 없는 것으로 본다. 404 로 돌려줘야
	 * 그 choreId 가 누구 것인지 짚어 보는 것을 막을 수 있다.
	 */
	private Chore findChore(Integer familyId, Integer childId, Integer choreId) {
		Chore chore = choreRepository.findByIdAndFamilyId(choreId, familyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_NOT_FOUND));
		if (!chore.getChildId().equals(childId)) {
			throw new BusinessException(ErrorCode.CHORE_NOT_FOUND);
		}
		return chore;
	}
}
