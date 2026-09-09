package com.bana.dotori.chore.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.chore.dto.ChoreApprovalResponse;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreCompletionRequest;
import com.bana.dotori.chore.repository.ChoreCompletionRequestRepository;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointWalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChoreApprovalServiceImpl implements ChoreApprovalService {

	/**
	 * 승인 시각을 KST 로 고정한다. 이유는 {@code ChoreServiceImpl.KST} 주석과 같다.
	 *
	 * <p>여기서는 특히 중요하다. processed_at 이 주기당 지급 여부를 판정하는 값이고
	 * ({@code ChoreCompletionRequestRepository#hasApprovedSince}), 그 판정의 경계는
	 * {@code Chore#currentPeriodStartedAt()} 이 만든 KST 자정이다. 기록만 UTC 로 남으면
	 * 한국 시간 자정 직후 승인이 지난 주기로 분류돼 같은 주기에 두 번 지급된다.
	 */
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private static final String REWARD_DESCRIPTION_PREFIX = "집안일 보상: ";

	private final ChoreRepository choreRepository;
	private final ChoreCompletionRequestRepository completionRequestRepository;
	private final ChoreAccessValidator accessValidator;
	private final PointWalletService pointWalletService;

	/**
	 * 부모가 완료를 승인하고 보상을 지급한다 (CHORE-09 + CHORE-07).
	 *
	 * <p>상태 변경과 지급이 <b>한 트랜잭션</b>이다. 나누면 "승인은 됐는데 포인트는 안 들어온"
	 * 상태가 남고, 그건 아이 입장에서 되돌릴 방법이 없는 사고다.
	 *
	 * <p>기간이 지났는지, 할 일이 지워졌는지는 보지 않는다. 이미 올라온 인증은 부모가
	 * 처리할 수 있어야 한다 — 막는 것은 새 요청뿐이다.
	 *
	 * <p><b>주기당 1회 지급</b>은 지갑의 멱등키가 보장한다. 요청 단계에서도 한 번 걸러
	 * 내지만({@code ChoreCompletionServiceImpl}) 그 검사는 원자적이지 않아서, 같은 주기에
	 * 대기 중인 요청이 둘 있고 부모가 동시에 승인하면 둘 다 통과한다.
	 * 그때 두 번째는 {@code point_transactions} 의
	 * {@code UNIQUE (child_id, idempotency_key)} 에 걸려 이전 거래를 되돌려받고
	 * ({@code replayed}), 여기서 409 로 던져 트랜잭션 전체를 되돌린다.
	 * 던지지 않으면 지급 없이 SUCCEED 로 바뀐 요청이 남는다.
	 */
	@Override
	@Transactional
	public ChoreApprovalResponse approve(Integer userId, Integer familyId, Integer childId, Integer requestId) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireParent(userId);

		ChoreCompletionRequest completionRequest = findRequest(childId, requestId);
		Chore chore = findChore(familyId, childId, completionRequest.getChoreId());

		Integer rewardPoints = chore.getRewardPoints();
		if (rewardPoints == null) {
			// 요청 단계에서 막았으므로 정상 흐름에서는 오지 않는다. AI 산정 전에 들어온
			// 요청이 남아 있거나 부모가 금액을 비운 경우를 대비한 방어다.
			throw new BusinessException(ErrorCode.REWARD_NOT_EVALUATED,
					"보상이 산정되지 않아 승인할 수 없습니다. choreId=" + chore.getId());
		}

		// 상태 전이를 먼저 한다. PENDING 이 아니면 여기서 409 가 나가고 지갑 락을 잡지 않는다.
		OffsetDateTime processedAt = OffsetDateTime.now(KST);
		completionRequest.approve(processedAt);

		// 첫 보상이 첫 포인트일 수 있다. 지갑이 없으면 deposit 이 WALLET_NOT_FOUND 로 터진다.
		pointWalletService.createWalletIfAbsent(childId);

		PointChangeResult result = pointWalletService.deposit(PointChangeCommand.of(
				childId, rewardPoints, PointTransactionReason.CHORE_REWARD,
				REWARD_DESCRIPTION_PREFIX + chore.getTitle(), chore.rewardIdempotencyKey()));

		if (result.replayed()) {
			throw new BusinessException(ErrorCode.ALREADY_REWARDED,
					"이번 주기에 이미 지급됐습니다. choreId=" + chore.getId()
							+ ", 주기시작=" + chore.getStartDate() + ", requestId=" + requestId);
		}

		log.info("집안일 보상 지급 완료. familyId={}, childId={}, choreId={}, requestId={}, amount={}, balanceAfter={}",
				familyId, childId, chore.getId(), requestId, rewardPoints, result.balanceAfter());

		return new ChoreApprovalResponse(completionRequest.getId(), completionRequest.getStatus(),
				rewardPoints, processedAt);
	}

	/**
	 * 경로가 지목한 아이의 요청을 집는다.
	 *
	 * <p>요청 ID 만으로 찾고 아이가 어긋나면 없는 것으로 본다. 403 을 주면 그 requestId 가
	 * 실재한다는 사실이 새어 나가고, 부모는 남의 집 요청 ID 를 알 이유가 없다.
	 */
	private ChoreCompletionRequest findRequest(Integer childId, Integer requestId) {
		ChoreCompletionRequest completionRequest = completionRequestRepository.findByIdForUpdate(requestId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND));
		if (!completionRequest.getChildId().equals(childId)) {
			throw new BusinessException(ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND);
		}
		return completionRequest;
	}

	/**
	 * 요청이 가리키는 할 일을 가족 경계 안에서 집는다.
	 *
	 * <p>지워진 할 일도 찾는다. 부모가 할 일을 지웠어도 이미 올라온 인증은 처리해 줘야 한다.
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
