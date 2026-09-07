package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.mock.bank.BankTransferClient;
import com.bana.dotori.mock.bank.BankTransferResult;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;

/**
 * 서비스 계층 - 충전의 조립 순서를 검증한다.
 *
 * <p>여기가 실패하면 원인은 잔액 계산도 HTTP 매핑도 아니고
 * "포인트 발행과 송금을 어떤 순서로, 어떤 조건에 부르는가"다.
 */
@DisplayName("PointChargeService 충전 흐름")
@ExtendWith(MockitoExtension.class)
class PointChargeServiceTest {

	private static final Integer FAMILY_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final String KEY = "charge-001";

	@Mock
	private PointWalletService pointWalletService;

	@Mock
	private BankTransferClient bankTransferClient;

	@Mock
	private FamilyMembershipReader familyMembershipReader;

	@InjectMocks
	private PointChargeService pointChargeService;

	@Test
	@DisplayName("포인트를 CHARGE 사유로 발행한 뒤 송금한다")
	void 포인트_발행_후_송금한다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(pointWalletService.deposit(any())).willReturn(applied(1_000, 6_000));
		given(bankTransferClient.transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.willReturn(new BankTransferResult("MOCK-1", 10_000));

		PointChangeResult result = pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY);

		ArgumentCaptor<PointChangeCommand> captor = ArgumentCaptor.forClass(PointChangeCommand.class);
		verify(pointWalletService).deposit(captor.capture());
		assertThat(captor.getValue().reason()).isEqualTo(PointTransactionReason.CHARGE);
		assertThat(captor.getValue().amount())
				.as("현금 10원 = 포인트 1점이므로 10,000원 요청은 1,000포인트로 발행돼야 한다")
				.isEqualTo(1_000);
		assertThat(captor.getValue().idempotencyKey()).isEqualTo(KEY);
		assertThat(result.balanceAfter()).isEqualTo(6_000);

		// 검증을 모두 통과한 뒤에 돈을 움직여야 한다.
		InOrder inOrder = inOrder(pointWalletService, bankTransferClient);
		inOrder.verify(pointWalletService).deposit(any());
		inOrder.verify(bankTransferClient).transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000, KEY);
	}

	@Test
	@DisplayName("포인트 발행이 실패하면 송금을 시도조차 하지 않는다")
	void 포인트_발행_실패시_송금하지_않는다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		willThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND, "지갑 없음")).given(pointWalletService).deposit(any());

		assertThatThrownBy(() -> pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_NOT_FOUND);

		verify(bankTransferClient, never()).transferFromChildToParent(any(), any(), anyInt(), any());
	}

	@Test
	@DisplayName("송금이 실패하면 예외가 그대로 전파돼 트랜잭션이 롤백된다")
	void 송금_실패는_전파된다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(pointWalletService.deposit(any())).willReturn(applied(1_000, 6_000));
		willThrow(new BusinessException(ErrorCode.INSUFFICIENT_ACCOUNT_BALANCE, "계좌 잔액 부족"))
				.given(bankTransferClient).transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000, KEY);

		assertThatThrownBy(() -> pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.as("여기서 예외를 삼키면 포인트만 발행되고 돈은 안 옮겨진다")
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_ACCOUNT_BALANCE);
	}

	@Test
	@DisplayName("이미 처리된 요청이면 송금을 다시 보내지 않는다")
	void 중복_요청은_송금하지_않는다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(pointWalletService.deposit(any())).willReturn(replayed(1_000, 6_000));

		PointChangeResult result = pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY);

		assertThat(result.replayed()).isTrue();
		verify(bankTransferClient, never()).transferFromChildToParent(any(), any(), anyInt(), any());
	}

	@Test
	@DisplayName("이체 금액이 요청 금액과 다르면 실패로 처리한다")
	void 부분_송금은_실패다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(pointWalletService.deposit(any())).willReturn(applied(1_000, 6_000));
		given(bankTransferClient.transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.willReturn(new BankTransferResult("MOCK-1", 9_000));

		assertThatThrownBy(() -> pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.as("발행한 포인트와 옮겨진 돈이 다르면 성공이 아니다")
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.BANK_TRANSFER_FAILED);
	}

	private PointChangeResult applied(int amount, int balanceAfter) {
		return result(amount, balanceAfter, false);
	}

	private PointChangeResult replayed(int amount, int balanceAfter) {
		return result(amount, balanceAfter, true);
	}

	private PointChangeResult result(int amount, int balanceAfter, boolean replayed) {
		return new PointChangeResult(100, 10, PointTransactionReason.CHARGE,
				amount, balanceAfter, OffsetDateTime.now(), replayed);
	}

	@Test
	@DisplayName("충전_실패_가족_자녀가_아니면_송금도_발행도_하지_않는다")
	void 충전_실패_가족_자녀가_아니면_송금도_발행도_하지_않는다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(false);

		assertThatThrownBy(() -> pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.as("familyId 가 송금 받을 부모 계좌를 정하므로, 소속을 확인하지 않으면 남의 집으로 돈이 간다")
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FAMILY_CHILD);

		verify(pointWalletService, never()).deposit(any());
		verify(bankTransferClient, never()).transferFromChildToParent(any(), any(), anyInt(), any());
	}

	@Test
	@DisplayName("환전_10원당_1포인트로_발행한다")
	void 환전_10원당_1포인트로_발행한다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(pointWalletService.deposit(any())).willReturn(applied(5, 5));
		given(bankTransferClient.transferFromChildToParent(FAMILY_ID, CHILD_ID, 50, KEY))
				.willReturn(new BankTransferResult("MOCK-1", 50));

		pointChargeService.charge(FAMILY_ID, CHILD_ID, 50, KEY);

		ArgumentCaptor<PointChangeCommand> captor = ArgumentCaptor.forClass(PointChangeCommand.class);
		verify(pointWalletService).deposit(captor.capture());
		assertThat(captor.getValue().amount()).as("50원 → 5포인트").isEqualTo(5);

		// 송금은 포인트가 아니라 현금 금액으로 나가야 한다. 여기가 뒤바뀌면 10분의 1만 빠진다.
		verify(bankTransferClient).transferFromChildToParent(FAMILY_ID, CHILD_ID, 50, KEY);
	}

	@Test
	@DisplayName("환전_10원_단위가_아니면_송금도_발행도_하지_않는다")
	void 환전_10원_단위가_아니면_거부한다() {
		assertThatThrownBy(() -> pointChargeService.charge(FAMILY_ID, CHILD_ID, 1_005, KEY))
				.as("버림하면 잔돈이 계좌에서만 빠지고 포인트로 돌아오지 않는다")
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CHARGE_UNIT);

		verify(pointWalletService, never()).deposit(any());
		verify(bankTransferClient, never()).transferFromChildToParent(any(), any(), anyInt(), any());
	}

	@Test
	@DisplayName("환전_10원_미만은_거부한다")
	void 환전_10원_미만은_거부한다() {
		assertThatThrownBy(() -> pointChargeService.charge(FAMILY_ID, CHILD_ID, 9, KEY))
				.as("1포인트도 안 되는 금액은 받을 수 없다")
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CHARGE_UNIT);

		verify(pointWalletService, never()).deposit(any());
	}

	@Test
	@DisplayName("첫_충전이면_지갑을_만들고_발행한다")
	void 첫_충전이면_지갑을_먼저_만든다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(pointWalletService.deposit(any())).willReturn(applied(1_000, 1_000));
		given(bankTransferClient.transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.willReturn(new BankTransferResult("MOCK-1", 10_000));

		pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY);

		// 가입 직후 아이는 지갑 행이 없다. 이 호출이 빠지면 첫 충전이 WALLET_NOT_FOUND 로 막힌다.
		// 순서도 중요하다. 가족 소속 확인 전에 만들면 아무 userId 로나 지갑을 만들 수 있다.
		InOrder inOrder = inOrder(familyMembershipReader, pointWalletService);
		inOrder.verify(familyMembershipReader).isChildOfFamily(FAMILY_ID, CHILD_ID);
		inOrder.verify(pointWalletService).createWalletIfAbsent(CHILD_ID);
		inOrder.verify(pointWalletService).deposit(any());
	}

	@Test
	@DisplayName("가족_자녀가_아니면_지갑도_만들지_않는다")
	void 가족_자녀가_아니면_지갑도_만들지_않는다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(false);

		assertThatThrownBy(() -> pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.isInstanceOf(BusinessException.class);

		verify(pointWalletService, never()).createWalletIfAbsent(any());
	}

	@Test
	@DisplayName("충전_순서_가족_확인을_포인트_발행보다_먼저_한다")
	void 충전_순서_가족_확인을_포인트_발행보다_먼저_한다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(pointWalletService.deposit(any())).willReturn(applied(1_000, 6_000));
		given(bankTransferClient.transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.willReturn(new BankTransferResult("MOCK-1", 10_000));

		pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY);

		InOrder inOrder = inOrder(familyMembershipReader, pointWalletService, bankTransferClient);
		inOrder.verify(familyMembershipReader).isChildOfFamily(FAMILY_ID, CHILD_ID);
		inOrder.verify(pointWalletService).deposit(any());
		inOrder.verify(bankTransferClient).transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000, KEY);
	}
}
