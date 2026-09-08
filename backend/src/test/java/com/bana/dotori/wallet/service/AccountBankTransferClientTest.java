package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.account.dto.TransferRequest;
import com.bana.dotori.account.dto.TransferResponse;
import com.bana.dotori.account.service.AccountService;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.mock.bank.BankTransferResult;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;

/**
 * 실제 연동 계좌를 통한 자녀→부모 송금이 계좌 서비스에 올바르게 위임되는지 확인한다.
 */
@DisplayName("AccountBankTransferClient 실제 송금 위임")
@ExtendWith(MockitoExtension.class)
class AccountBankTransferClientTest {

	private static final Integer FAMILY_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final Integer PARENT_ID = 9;
	private static final String PARENT_ACCOUNT_NO = "0016174648358792";
	private static final String KEY = "charge-001";

	@Mock
	private AccountService accountService;

	@Mock
	private FamilyMembershipReader familyMembershipReader;

	@InjectMocks
	private AccountBankTransferClient bankTransferClient;

	@Test
	@DisplayName("부모 계좌를 찾아 아이 명의로 이체를 요청한다")
	void 부모_계좌로_이체를_위임한다() {
		given(familyMembershipReader.findParentUserId(FAMILY_ID)).willReturn(PARENT_ID);
		given(accountService.getLinkedAccountNumber(PARENT_ID)).willReturn(PARENT_ACCOUNT_NO);
		given(accountService.transfer(CHILD_ID, new TransferRequest(PARENT_ACCOUNT_NO, 10_000L, "포인트 충전"), KEY))
				.willReturn(new TransferResponse("0011214764051239", PARENT_ACCOUNT_NO, 10_000L, OffsetDateTime.now()));

		BankTransferResult result = bankTransferClient.transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000, KEY);

		assertThat(result.transferredAmount()).isEqualTo(10_000);
		assertThat(result.externalTransactionId()).isEqualTo(KEY);
		then(accountService).should()
				.transfer(CHILD_ID, new TransferRequest(PARENT_ACCOUNT_NO, 10_000L, "포인트 충전"), KEY);
	}

	@Test
	@DisplayName("idempotencyKey가 없으면 임의의 거래 식별자를 만든다")
	void idempotencyKey_없으면_임의_식별자를_만든다() {
		given(familyMembershipReader.findParentUserId(FAMILY_ID)).willReturn(PARENT_ID);
		given(accountService.getLinkedAccountNumber(PARENT_ID)).willReturn(PARENT_ACCOUNT_NO);
		given(accountService.transfer(CHILD_ID, new TransferRequest(PARENT_ACCOUNT_NO, 10_000L, "포인트 충전"), null))
				.willReturn(new TransferResponse("0011214764051239", PARENT_ACCOUNT_NO, 10_000L, OffsetDateTime.now()));

		BankTransferResult result = bankTransferClient.transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000, null);

		assertThat(result.externalTransactionId()).isNotBlank();
	}

	@Test
	@DisplayName("가족에 부모가 없으면 ACCOUNT_NOT_LINKED 예외가 발생한다")
	void 부모가_없으면_예외가_발생한다() {
		given(familyMembershipReader.findParentUserId(FAMILY_ID)).willReturn(null);

		assertThatThrownBy(() -> bankTransferClient.transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_LINKED);

		then(accountService).shouldHaveNoInteractions();
	}
}
