package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;

/**
 * 서비스 계층 - 지급의 검증 순서와 위임을 확인한다.
 *
 * <p>여기가 실패하면 원인은 잔액 계산도 HTTP 매핑도 아니고
 * "가족 소속을 언제 확인하고 무엇을 공통 서비스에 넘기는가"다.
 */
@DisplayName("PointGrantService 지급 흐름")
@ExtendWith(MockitoExtension.class)
class PointGrantServiceTest {

	private static final Integer PARENT_ID = 9;
	private static final Integer FAMILY_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final String KEY = "grant-001";

	@Mock
	private PointWalletService pointWalletService;

	@Mock
	private FamilyMembershipReader familyMembershipReader;

	@InjectMocks
	private PointGrantService pointGrantService;

	@Test
	@DisplayName("가족 자녀에게 GRANT 사유로 포인트를 발행한다")
	void 포인트를_발행한다() {
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(pointWalletService.deposit(any())).willReturn(result(5_000, 20_000));

		PointChangeResult result = pointGrantService.grant(PARENT_ID, FAMILY_ID, CHILD_ID, 5_000, "용돈", KEY);

		ArgumentCaptor<PointChangeCommand> captor = ArgumentCaptor.forClass(PointChangeCommand.class);
		verify(pointWalletService).deposit(captor.capture());
		assertThat(captor.getValue().reason()).isEqualTo(PointTransactionReason.GRANT);
		assertThat(captor.getValue().userId()).isEqualTo(CHILD_ID);
		assertThat(captor.getValue().amount()).isEqualTo(5_000);
		assertThat(captor.getValue().description()).isEqualTo("용돈");
		assertThat(captor.getValue().idempotencyKey()).isEqualTo(KEY);
		assertThat(result.balanceAfter()).isEqualTo(20_000);
	}

	@Test
	@DisplayName("가족 자녀가 아니면 포인트를 발행하지 않고 403 예외를 던진다")
	void 가족_자녀가_아니면_거부한다() {
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(false);

		assertThatThrownBy(() -> pointGrantService.grant(PARENT_ID, FAMILY_ID, CHILD_ID, 5_000, "용돈", KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FAMILY_CHILD);

		verify(pointWalletService, never()).deposit(any());
	}

	@Test
	@DisplayName("가족 소속 확인을 포인트 발행보다 먼저 한다")
	void 소속_확인이_먼저다() {
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(pointWalletService.deposit(any())).willReturn(result(5_000, 20_000));

		pointGrantService.grant(PARENT_ID, FAMILY_ID, CHILD_ID, 5_000, "용돈", KEY);

		// 발행이 먼저 일어나면, 남의 집 아이 지갑에 포인트가 들어갔다가 롤백되는 셈이 된다.
		InOrder inOrder = inOrder(familyMembershipReader, pointWalletService);
		inOrder.verify(familyMembershipReader).isChildOfFamily(FAMILY_ID, CHILD_ID);
		inOrder.verify(pointWalletService).deposit(any());
	}

	@ParameterizedTest(name = "사유가 [{0}] 이면 기본 문구로 채운다")
	@NullAndEmptySource
	@ValueSource(strings = {"   "})
	@DisplayName("지급 사유가 비어 있으면 기본 문구를 넣는다")
	void 사유가_비면_기본값을_쓴다(String blankDescription) {
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(pointWalletService.deposit(any())).willReturn(result(5_000, 20_000));

		pointGrantService.grant(PARENT_ID, FAMILY_ID, CHILD_ID, 5_000, blankDescription, KEY);

		ArgumentCaptor<PointChangeCommand> captor = ArgumentCaptor.forClass(PointChangeCommand.class);
		verify(pointWalletService).deposit(captor.capture());
		assertThat(captor.getValue().description())
				.as("거래내역 화면이 빈 칸으로 보이면 안 된다")
				.isEqualTo("부모 지급");
	}

	@Test
	@DisplayName("공통 서비스가 던진 예외는 그대로 전파된다")
	void 공통_서비스_예외가_전파된다() {
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		willThrow(new BusinessException(ErrorCode.POINT_BALANCE_EXCEEDED, "상한 초과"))
				.given(pointWalletService).deposit(any());

		assertThatThrownBy(() -> pointGrantService.grant(PARENT_ID, FAMILY_ID, CHILD_ID, 5_000, "용돈", KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_BALANCE_EXCEEDED);
	}

	private PointChangeResult result(int amount, int balanceAfter) {
		return new PointChangeResult(101, 10, PointTransactionReason.GRANT,
				amount, balanceAfter, OffsetDateTime.now(), false);
	}
}
