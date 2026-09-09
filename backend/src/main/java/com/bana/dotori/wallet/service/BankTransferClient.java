package com.bana.dotori.wallet.service;

/**
 * 아이 계좌에서 부모 계좌로 실제 금액을 옮기는 외부 금융 API 경계.
 *
 * <p>계좌번호를 인자로 받지 않는 이유는, 어느 계좌에서 어느 계좌로 보낼지 정하는 것이
 * linked_accounts 를 아는 구현체({@link AccountBankTransferClient})의 책임이기 때문이다.
 * 충전 서비스는 "누가 얼마를"만 안다.
 */
public interface BankTransferClient {

	/**
	 * 아이의 연동 계좌에서 부모의 연동 계좌로 송금한다.
	 *
	 * @param familyId       대상 가족
	 * @param childId        송금하는 아이
	 * @param amount         송금액(원). 항상 양수
	 * @param idempotencyKey 중복 제출 방지 키. 없으면 null
	 * @return 송금 결과
	 * @throws com.bana.dotori.global.error.BusinessException
	 *         ACCOUNT_NOT_LINKED 아이 또는 부모의 연동 계좌가 없는 경우(명세상 404),
	 *         INSUFFICIENT_ACCOUNT_BALANCE 아이 계좌 잔액이 부족한 경우(명세상 400),
	 *         BANK_TRANSFER_FAILED 그 밖의 이유로 송금이 실패한 경우
	 */
	BankTransferResult transferFromChildToParent(Integer familyId, Integer childId, int amount, String idempotencyKey);
}
