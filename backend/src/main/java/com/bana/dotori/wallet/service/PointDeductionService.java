package com.bana.dotori.wallet.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.exception.InvalidTransactionReasonException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WLT-05 포인트 차감.
 *
 * <p>충전·지급과 달리 이 기능에는 API 가 없다. 명세대로 구매·환전·예적금·투자 같은
 * 다른 도메인의 처리 안에서 호출되는 내부 서비스다.
 *
 * <p>차감을 도메인마다 각자 {@link PointWalletService#withdraw} 로 직접 부르게 두지 않고
 * 이 서비스를 한 겹 둔 이유는 두 가지다.
 *
 * <ol>
 *   <li>사유와 방향이 어긋난 호출을 막는다. GRANT 사유로 차감하는 실수를 여기서 잡지 않으면
 *       원장에 "지급인데 잔액이 줄어든" 줄이 남고 RPT-02 통계가 뒤틀린다.</li>
 *   <li>차감 호출부가 한 곳으로 모여, 나중에 한도·정책 같은 공통 규칙을 붙일 자리가 생긴다.</li>
 * </ol>
 *
 * <p>호출자는 자기 트랜잭션 안에서 이 메서드를 부른다. 전파 설정이 REQUIRED 라
 * 구매 저장과 포인트 차감이 하나의 트랜잭션으로 묶이고, 어느 한쪽이 실패하면 함께 롤백된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointDeductionService {

	private final PointWalletService pointWalletService;

	/**
	 * 승인된 사유로 포인트를 차감한다.
	 *
	 * @param childId        차감 대상 아이
	 * @param amount         차감 금액. 항상 양수
	 * @param reason         차감 사유. 차감이 허용된 사유여야 한다
	 * @param description    거래내역에 노출될 설명
	 * @param idempotencyKey 중복 차감 방지 키. 호출 도메인의 식별자를 넣는 것을 권장한다
	 *                       (예: purchase-123). 그래야 재시도해도 두 번 빠지지 않는다
	 * @throws InvalidTransactionReasonException 차감이 허용되지 않은 사유인 경우
	 * @throws com.bana.dotori.wallet.exception.InsufficientPointException 잔액이 부족한 경우
	 */
	@Transactional
	public PointChangeResult deduct(Integer childId, int amount, PointTransactionReason reason,
			String description, String idempotencyKey) {

		if (!reason.isWithdrawalAllowed()) {
			throw new InvalidTransactionReasonException(reason, "WITHDRAWAL");
		}

		PointChangeResult result = pointWalletService.withdraw(
				PointChangeCommand.of(childId, amount, reason, description, idempotencyKey));

		log.info("포인트 차감 완료. childId={}, amount={}, reason={}, balanceAfter={}, replayed={}",
				childId, amount, reason, result.balanceAfter(), result.replayed());
		return result;
	}

	/**
	 * 차감을 되돌린다. 구매 취소나 요청 반려처럼 되돌려 줘야 하는 경우에 쓴다.
	 *
	 * <p>이미 남긴 차감 이력을 지우지 않고 반대 방향 거래를 새로 남긴다.
	 * 원장은 고쳐 쓰지 않고 쌓기만 해야 나중에 무슨 일이 있었는지 추적할 수 있다.
	 */
	@Transactional
	public PointChangeResult refund(Integer childId, int amount, String description, String idempotencyKey) {
		PointChangeResult result = pointWalletService.deposit(PointChangeCommand.of(
				childId, amount, PointTransactionReason.REFUND, description, idempotencyKey));

		log.info("포인트 환급 완료. childId={}, amount={}, balanceAfter={}", childId, amount, result.balanceAfter());
		return result;
	}
}
