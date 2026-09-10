package com.bana.dotori.wallet.service;

/**
 * 송금 성공 결과.
 *
 * @param externalTransactionId 은행이 발급한 거래 식별자. 대사(reconciliation)와 문의 대응에 쓴다
 * @param transferredAmount     실제로 옮겨진 금액(원)
 */
public record BankTransferResult(
		String externalTransactionId,
		int transferredAmount) {
}
