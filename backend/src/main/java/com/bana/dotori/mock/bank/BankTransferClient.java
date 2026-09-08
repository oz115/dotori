package com.bana.dotori.mock.bank;

/**
 * 아이 계좌에서 부모 계좌로 실제 금액을 옮기는 외부 금융 API 경계.
 *
 * <p>계좌 연동(ACC-01·02, S15P21A505-4·13)이 아직 끝나지 않아 지금은
 * {@link MockBankTransferClient} 만 있다. 충전 로직이 은행 API 세부사항에 물리지 않도록
 * 인터페이스를 먼저 두었고, 실제 싸피 금융망 연동이 들어오면 구현체만 갈아끼우면 된다.
 *
 * <p>계좌번호를 인자로 받지 않는 이유는, 어느 계좌에서 어느 계좌로 보낼지 정하는 것이
 * linked_accounts 를 아는 구현체의 책임이기 때문이다. 충전 서비스는 "누가 얼마를"만 안다.
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
