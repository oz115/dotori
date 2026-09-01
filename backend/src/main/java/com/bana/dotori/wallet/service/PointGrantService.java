package com.bana.dotori.wallet.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.wallet.domain.PointTransactionReason;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.exception.NotFamilyChildException;
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
	public PointChangeResult grant(Integer familyId, Integer childId, int amount, String description,
			String idempotencyKey) {

		if (!familyMembershipReader.isChildOfFamily(familyId, childId)) {
			throw new NotFamilyChildException(familyId, childId);
		}

		PointChangeResult result = pointWalletService.deposit(PointChangeCommand.of(
				childId, amount, PointTransactionReason.GRANT, resolveDescription(description), idempotencyKey));

		log.info("포인트 지급 완료. familyId={}, childId={}, amount={}, balanceAfter={}, replayed={}",
				familyId, childId, amount, result.balanceAfter(), result.replayed());
		return result;
	}

	private String resolveDescription(String description) {
		if (description == null || description.isBlank()) {
			return DEFAULT_DESCRIPTION;
		}
		return description;
	}
}
