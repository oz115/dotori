package com.bana.dotori.wallet.service;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.bana.dotori.account.dto.TransferRequest;
import com.bana.dotori.account.dto.TransferResponse;
import com.bana.dotori.account.service.AccountService;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.mock.bank.BankTransferClient;
import com.bana.dotori.mock.bank.BankTransferResult;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;

import lombok.RequiredArgsConstructor;

/**
 * 실제 연동 계좌를 통해 아이 계좌에서 부모 계좌로 송금하는 {@link BankTransferClient} 구현체.
 *
 * <p>계좌 자체를 모르는 {@link com.bana.dotori.wallet.service.PointChargeService}를 대신해
 * 부모 계좌를 찾고, {@link AccountService#transfer}로 실제 금융망 이체를 실행한다.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AccountBankTransferClient implements BankTransferClient {

	private static final String TRANSFER_MEMO = "포인트 충전";

	private final AccountService accountService;
	private final FamilyMembershipReader familyMembershipReader;

	@Override
	public BankTransferResult transferFromChildToParent(Integer familyId, Integer childId, int amount,
			String idempotencyKey) {
		Integer parentUserId = familyMembershipReader.findParentUserId(familyId);
		if (parentUserId == null) {
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED,
					"가족의 부모를 찾을 수 없습니다. familyId=" + familyId);
		}

		String parentAccountNo = accountService.getLinkedAccountNumber(parentUserId);
		TransferResponse response = accountService.transfer(childId,
				new TransferRequest(parentAccountNo, (long)amount, TRANSFER_MEMO), idempotencyKey);

		String externalTransactionId = (idempotencyKey != null) ? idempotencyKey : UUID.randomUUID().toString();
		return new BankTransferResult(externalTransactionId, Math.toIntExact(response.amount()));
	}
}
