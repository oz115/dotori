package com.bana.dotori.wallet.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WLT-02 포인트 지급.
 *
 * <p>부모가 실제 송금 없이 임의 금액의 포인트를 발행해 자녀에게 준다.
 * 충전(WLT-01)과 달리 은행을 거치지 않으므로 외부 호출이 없고, 트랜잭션도 짧다.
 *
 * <p>대신 여기서만 필요한 검증이 있다. 발행은 돈을 무에서 만들어 내는 일이라
 * 대상이 정말 그 가족의 자녀인지 반드시 확인해야 한다.
 * 확인하지 않으면 다른 가족 아이에게 포인트를 꽂아 넣을 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointGrantService {

	private static final String DEFAULT_DESCRIPTION = "부모 지급";

	private final PointWalletService pointWalletService;
	private final FamilyMembershipReader familyMembershipReader;

	@Transactional
	public PointChangeResult grant(Integer parentId, Integer familyId, Integer childId, int amount,
			String description, String idempotencyKey) {

		// 발행을 요청한 사람이 그 가족의 부모인지 먼저 본다. 이 확인이 없으면
		// 로그인만 되어 있으면 누구나 남의 집 아이에게 포인트를 꽂아 넣을 수 있다.
		if (!familyMembershipReader.isParentOfFamily(familyId, parentId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
					"해당 가족의 부모가 아닙니다. familyId=" + familyId + ", parentId=" + parentId);
		}

		if (!familyMembershipReader.isChildOfFamily(familyId, childId)) {
			throw new BusinessException(ErrorCode.NOT_FAMILY_CHILD,
					"해당 가족의 자녀가 아닙니다. familyId=" + familyId + ", childId=" + childId);
		}

		// 아직 한 번도 충전한 적 없는 아이는 지갑이 없다. 가족 소속을 확인한 뒤에 만든다.
		pointWalletService.createWalletIfAbsent(childId);

		PointChangeResult result = pointWalletService.deposit(PointChangeCommand.of(
				childId, amount, PointTransactionReason.GRANT, resolveDescription(description), idempotencyKey));

		log.info("포인트 지급 완료. familyId={}, parentId={}, childId={}, amount={}, balanceAfter={}, replayed={}",
				familyId, parentId, childId, amount, result.balanceAfter(), result.replayed());
		return result;
	}

	private String resolveDescription(String description) {
		if (description == null || description.isBlank()) {
			return DEFAULT_DESCRIPTION;
		}
		return description;
	}
}
