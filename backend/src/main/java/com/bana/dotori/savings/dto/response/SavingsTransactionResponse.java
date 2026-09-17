package com.bana.dotori.savings.dto.response;

import java.time.OffsetDateTime;

import com.bana.dotori.savings.entity.SavingsProductType;
import com.bana.dotori.savings.entity.SavingsTransactionType;

/**
 * GET .../savings/transactions 응답 한 건 — 아이의 예적금에서 일어난 거래 한 줄.
 * 금액은 항상 양수이고 방향(지갑에서 나갔는지/들어왔는지)은 {@code transactionType} 이 정한다.
 * 화면이 어느 상품의 일인지 바로 그릴 수 있게 상품 이름·유형을 함께 담는다.
 */
public record SavingsTransactionResponse(
	Integer savingsTransactionId,
	Integer savingsSubscriptionId,
	Integer savingsProductId,
	String productName,
	SavingsProductType productType,
	SavingsTransactionType transactionType,
	int amount,
	OffsetDateTime createdAt
) {
}
