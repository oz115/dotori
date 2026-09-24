package com.bana.dotori.wallet.service;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.bana.dotori.account.dto.TransferRequest;
import com.bana.dotori.account.dto.TransferResponse;
import com.bana.dotori.account.service.AccountService;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.repository.UserRepository;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;

import lombok.RequiredArgsConstructor;

/**
 * 실제 연동 계좌를 통해 아이 계좌↔부모 계좌 간 송금을 수행하는 {@link BankTransferClient} 구현체.
 *
 * <p>계좌 자체를 모르는 {@link com.bana.dotori.wallet.service.PointChargeService}(충전) ·
 * {@code PointCashoutRequestService}(환전 승인)를 대신해 상대 계좌를 찾고,
 * {@link AccountService#transfer}로 실제 금융망 이체를 실행한다.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AccountBankTransferClient implements BankTransferClient {

	private static final String CHARGE_TRANSFER_MEMO = "포인트 충전";
	private static final String CHARGE_DEPOSIT_SUMMARY_SUFFIX = " 도토리충전";
	private static final String CASHOUT_TRANSFER_MEMO = "포인트 환전";

	private final AccountService accountService;
	private final FamilyMembershipReader familyMembershipReader;
	private final UserRepository userRepository;

	@Override
	public BankTransferResult transferFromChildToParent(Integer familyId, Integer childId, int amount,
			String idempotencyKey) {
		Integer parentUserId = familyMembershipReader.findParentUserId(familyId);
		if (parentUserId == null) {
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED,
					"가족의 부모를 찾을 수 없습니다. familyId=" + familyId);
		}

		String parentAccountNo = accountService.getLinkedAccountNumber(parentUserId);
		// 부모 계좌 거래내역에는 '자녀이름 도토리충전'으로 표시한다
		String childName = userRepository.findById(childId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
				.getName();
		TransferResponse response = accountService.transfer(childId,
				new TransferRequest(parentAccountNo, (long)amount, CHARGE_TRANSFER_MEMO), idempotencyKey,
				childName + CHARGE_DEPOSIT_SUMMARY_SUFFIX);

		return toResult(response, idempotencyKey);
	}

	@Override
	public BankTransferResult transferFromParentToChild(Integer parentId, Integer childId, int amount,
			String idempotencyKey) {
		String childAccountNo = accountService.getLinkedAccountNumber(childId);
		TransferResponse response = accountService.transfer(parentId,
				new TransferRequest(childAccountNo, (long)amount, CASHOUT_TRANSFER_MEMO), idempotencyKey);

		return toResult(response, idempotencyKey);
	}

	private BankTransferResult toResult(TransferResponse response, String idempotencyKey) {
		String externalTransactionId = (idempotencyKey != null) ? idempotencyKey : UUID.randomUUID().toString();
		return new BankTransferResult(externalTransactionId, Math.toIntExact(response.amount()));
	}
}
