package com.bana.dotori.savings.service;

import java.util.List;

import com.bana.dotori.savings.dto.request.SavingsContributeRequest;
import com.bana.dotori.savings.dto.request.SavingsSubscribeRequest;
import com.bana.dotori.savings.dto.response.SavingsExpectedReturnResponse;
import com.bana.dotori.savings.dto.response.SavingsProductResponse;
import com.bana.dotori.savings.dto.response.SavingsSubscriptionResponse;
import com.bana.dotori.savings.dto.response.SavingsSummaryResponse;
import com.bana.dotori.savings.dto.response.SavingsTransactionResponse;

public interface SavingsService {

	/** 6개 고정 상품 목록. 이 가족의 weeklyRewardLimit 기준으로 계산한 가입 가능 최대 금액을 함께 준다 */
	List<SavingsProductResponse> listProducts(Integer userId, Integer familyId, Integer childId);

	/** SAV-02 상품 하나의 상세. 목록과 같은 내용이며 이 가족 기준 최대 금액도 함께 준다 */
	SavingsProductResponse getProduct(Integer userId, Integer familyId, Integer childId, Integer savingsProductId);

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

	/**
	 * 부모 홈 "자녀 예적금" 카드용 요약. 진행 중·수령 대기 건만 집계한다 —
	 * 목록을 통째로 받아 클라이언트가 집계하지 않도록 한 번의 호출로 끝낸다.
	 */
	SavingsSummaryResponse getSummary(Integer userId, Integer familyId, Integer childId);

	/** 아이의 모든 가입 건에서 일어난 거래(납입·이자·수령·해지)를 최신순으로 준다 — 거래 이력 화면용 */
	List<SavingsTransactionResponse> listTransactions(Integer userId, Integer familyId, Integer childId);

	/** SAV-05 만기 예상 수령액과 지금 해지 시 수령액을 나란히 계산한다 */
	SavingsExpectedReturnResponse getExpectedReturn(Integer userId, Integer familyId, Integer childId,
		Integer savingsSubscriptionId);
}
