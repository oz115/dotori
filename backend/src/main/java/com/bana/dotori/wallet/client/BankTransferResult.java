package com.bana.dotori.wallet.client;

/**
 * 송금 성공 결과.
 *
 * @param externalTransactionId 은행이 발급한 거래 식별자. 대사(reconciliation)와 문의 대응에 쓴다
 * @param transferredAmount     실제로 옮겨진 금액(원)
 * @param childAccount          송금 후 아이 계좌 상태
 * @param parentAccount         입금 후 부모 계좌 상태
 */
public record BankTransferResult(
		String externalTransactionId,
		int transferredAmount,
		LinkedAccountSnapshot childAccount,
		LinkedAccountSnapshot parentAccount) {
}
