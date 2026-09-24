package com.bana.dotori.savings.dto.response;

import java.time.OffsetDateTime;

/**
 * SAV-05 예상 손익 조회 응답. "지금 이대로 두면 만기에 얼마를 받고, 지금 깨면 얼마를 받는가"를
 * 나란히 보여 주는 것이 이 API 의 목적이라 두 금액을 함께 담는다.
 *
 * @param currentAmount          지금까지 납입된 원금 누계
 * @param accruedInterest        지금까지 실제로 쌓인 이자
 * @param expectedInterest       만기까지 받게 될 이자 총액 (이미 쌓인 것 + 앞으로 쌓일 것).
 *                               보통예금은 만기가 없어 다음 정산 한 주치를 준다
 * @param expectedTotal          만기 예상 수령액 (원금 + 이자)
 * @param earlyTerminationAmount 지금 중도 해지하면 받는 금액. 이자를 포기하므로 원금뿐이다
 * @param maturityAt             만기 시각. 보통예금(무기한)은 null
 */
public record SavingsExpectedReturnResponse(
	Integer savingsSubscriptionId,
	int currentAmount,
	int accruedInterest,
	long expectedInterest,
	long expectedTotal,
	long earlyTerminationAmount,
	OffsetDateTime maturityAt
) {
}
