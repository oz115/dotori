package com.bana.dotori.savings.service;

import java.util.List;

import com.bana.dotori.savings.dto.request.SavingsContributeRequest;
import com.bana.dotori.savings.dto.request.SavingsSubscribeRequest;
import com.bana.dotori.savings.dto.response.SavingsProductResponse;
import com.bana.dotori.savings.dto.response.SavingsSubscriptionResponse;

public interface SavingsService {

	/** 6개 고정 상품 목록. 이 가족의 weeklyRewardLimit 기준으로 계산한 가입 가능 최대 금액을 함께 준다 */
	List<SavingsProductResponse> listProducts(Integer userId, Integer familyId, Integer childId);

	/** 예적금 가입. 원금(또는 회당 약정액)만큼 지갑에서 즉시 차감한다 — 목표적금만 차감이 없다 */
	SavingsSubscriptionResponse subscribe(Integer userId, Integer familyId, Integer childId,
		SavingsSubscribeRequest request, String idempotencyKey);

	/** 추가 납입. 목표적금이 이 납입으로 목표를 채우면 그 자리에서 만기 지급까지 끝난다 */
	SavingsSubscriptionResponse contribute(Integer userId, Integer familyId, Integer childId,
		Integer savingsSubscriptionId, SavingsContributeRequest request, String idempotencyKey);

	/** 만기 수령. 원금과 쌓인 이자를 지갑으로 돌려준다 */
	SavingsSubscriptionResponse receive(Integer userId, Integer familyId, Integer childId,
		Integer savingsSubscriptionId, String idempotencyKey);

	/** 중도 해지. 쌓인 이자는 포기하고 납입 원금만 돌려받는다 */
	SavingsSubscriptionResponse terminate(Integer userId, Integer familyId, Integer childId,
		Integer savingsSubscriptionId, String idempotencyKey);

	List<SavingsSubscriptionResponse> listSubscriptions(Integer userId, Integer familyId, Integer childId);

	SavingsSubscriptionResponse getSubscription(Integer userId, Integer familyId, Integer childId,
		Integer savingsSubscriptionId);
}
