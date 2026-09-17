package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.response.PointCashoutRequestResult;
import com.bana.dotori.wallet.entity.PointCashoutRequest;
import com.bana.dotori.wallet.entity.PointCashoutRequestStatus;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;
import com.bana.dotori.wallet.repository.PointCashoutRequestRepository;

/**
 * 서비스 계층 - 환전 요청/승인의 조립 순서를 검증한다.
 *
 * <p>특히 승인 중 송금이 실패했을 때 예외를 밖으로 던지지 않고 환급 후 FAILED 를
 * 정상 반환한다는 점(클래스 설명 참고)이 이 테스트의 핵심 검증 대상이다.
 */
@DisplayName("PointCashoutRequestService 환전 요청/승인 흐름")
@ExtendWith(MockitoExtension.class)
class PointCashoutRequestServiceTest {

	private static final Integer FAMILY_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final Integer PARENT_ID = 3;
	private static final Integer CASHOUT_REQUEST_ID = 10;
	private static final String KEY = "cashout-001";

	@Mock
	private PointDeductionService pointDeductionService;

	@Mock
	private PointCashoutRequestRepository pointCashoutRequestRepository;

	@Mock
	private FamilyMembershipReader familyMembershipReader;

	@Mock
	private BankTransferClient bankTransferClient;

	@Mock
	private PointWalletQueryService pointWalletQueryService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private PointCashoutRequestService pointCashoutRequestService;

	@Test
	@DisplayName("차감 후 PENDING 요청을 만든다")
	void 요청하면_차감후_PENDING_요청을_만든다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(familyMembershipReader.findParentUserId(FAMILY_ID)).willReturn(PARENT_ID);

		PointCashoutRequestResult result = pointCashoutRequestService.request(FAMILY_ID, CHILD_ID, 1_000, KEY);

		verify(pointDeductionService).deduct(eq(CHILD_ID), eq(1_000), any(), any(), eq(KEY));
		verify(pointCashoutRequestRepository).save(any(PointCashoutRequest.class));
		assertThat(result.status()).isEqualTo(PointCashoutRequestStatus.PENDING);
		assertThat(result.parentId()).isEqualTo(PARENT_ID);
		assertThat(result.pointAmount()).isEqualTo(1_000);
	}

	@Test
	@DisplayName("가족의 자녀가 아니면 차감도 요청 생성도 하지 않는다")
	void 가족_자녀가_아니면_차감하지_않는다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(false);

		assertThatThrownBy(() -> pointCashoutRequestService.request(FAMILY_ID, CHILD_ID, 1_000, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FAMILY_CHILD);

		verify(pointDeductionService, never()).deduct(any(), anyInt(), any(), any(), any());
		verify(pointCashoutRequestRepository, never()).save(any());
	}

	@Test
	@DisplayName("가족에 부모가 없으면 차감하지 않는다")
	void 부모가_없으면_차감하지_않는다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(familyMembershipReader.findParentUserId(FAMILY_ID)).willReturn(null);

		assertThatThrownBy(() -> pointCashoutRequestService.request(FAMILY_ID, CHILD_ID, 1_000, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_LINKED);

		verify(pointDeductionService, never()).deduct(any(), anyInt(), any(), any(), any());
	}

	@Test
	@DisplayName("잔액이 부족하면 요청 행이 생기지 않는다")
	void 잔액_부족시_요청행이_생기지_않는다() {
		given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(familyMembershipReader.findParentUserId(FAMILY_ID)).willReturn(PARENT_ID);
		willThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINT, "잔액 부족"))
				.given(pointDeductionService).deduct(any(), anyInt(), any(), any(), any());

		assertThatThrownBy(() -> pointCashoutRequestService.request(FAMILY_ID, CHILD_ID, 1_000, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_POINT);

		verify(pointCashoutRequestRepository, never()).save(any());
	}

	@Test
	@DisplayName("승인에 성공하면 SUCCEED 로 바뀌고 환급하지 않는다")
	void 승인_성공시_SUCCEED가_된다() {
		PointCashoutRequest request = PointCashoutRequest.pending(FAMILY_ID, CHILD_ID, PARENT_ID, 1_000);
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(CASHOUT_REQUEST_ID))
				.willReturn(Optional.of(request));
		given(bankTransferClient.transferFromParentToChild(PARENT_ID, CHILD_ID, 10_000, KEY))
				.willReturn(new BankTransferResult("MOCK-1", 10_000));

		PointCashoutRequestResult result = pointCashoutRequestService.approve(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, PARENT_ID, KEY);

		assertThat(result.status()).isEqualTo(PointCashoutRequestStatus.SUCCEED);
		assertThat(result.processedAt()).isNotNull();
		verify(pointDeductionService, never()).refund(any(), anyInt(), any(), any());
	}

	@Test
	@DisplayName("송금이 실패하면 예외 대신 환급 후 FAILED 를 돌려준다")
	void 송금_실패시_환급후_FAILED를_돌려준다() {
		PointCashoutRequest request = PointCashoutRequest.pending(FAMILY_ID, CHILD_ID, PARENT_ID, 1_000);
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(CASHOUT_REQUEST_ID))
				.willReturn(Optional.of(request));
		willThrow(new BusinessException(ErrorCode.INSUFFICIENT_ACCOUNT_BALANCE, "부모 계좌 잔액 부족"))
				.given(bankTransferClient).transferFromParentToChild(PARENT_ID, CHILD_ID, 10_000, KEY);

		PointCashoutRequestResult result = pointCashoutRequestService.approve(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, PARENT_ID, KEY);

		assertThat(result.status())
				.as("여기서 예외를 던지면 방금 하려는 환급까지 롤백된다")
				.isEqualTo(PointCashoutRequestStatus.FAILED);
		verify(pointDeductionService).refund(eq(CHILD_ID), eq(1_000), any(), any());
	}

	@Test
	@DisplayName("이체 금액이 요청 금액과 다르면 실패로 처리하고 환급한다")
	void 부분_송금은_실패로_처리하고_환급한다() {
		PointCashoutRequest request = PointCashoutRequest.pending(FAMILY_ID, CHILD_ID, PARENT_ID, 1_000);
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(CASHOUT_REQUEST_ID))
				.willReturn(Optional.of(request));
		given(bankTransferClient.transferFromParentToChild(PARENT_ID, CHILD_ID, 10_000, KEY))
				.willReturn(new BankTransferResult("MOCK-1", 9_000));

		PointCashoutRequestResult result = pointCashoutRequestService.approve(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, PARENT_ID, KEY);

		assertThat(result.status()).isEqualTo(PointCashoutRequestStatus.FAILED);
		verify(pointDeductionService).refund(eq(CHILD_ID), eq(1_000), any(), any());
	}

	@Test
	@DisplayName("해당 가족의 부모가 아니면 승인할 수 없다")
	void 가족의_부모가_아니면_승인할_수_없다() {
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(false);

		assertThatThrownBy(() -> pointCashoutRequestService.approve(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, PARENT_ID, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

		verify(pointCashoutRequestRepository, never()).findByCashoutRequestIdForUpdate(any());
	}

	@Test
	@DisplayName("요청이 없으면 404 다")
	void 요청이_없으면_404다() {
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(CASHOUT_REQUEST_ID))
				.willReturn(Optional.empty());

		assertThatThrownBy(() -> pointCashoutRequestService.approve(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, PARENT_ID, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CASHOUT_REQUEST_NOT_FOUND);
	}

	@Test
	@DisplayName("경로와 다른 요청이면 404 다")
	void 경로와_다른_요청이면_404다() {
		PointCashoutRequest request = PointCashoutRequest.pending(FAMILY_ID, 999, PARENT_ID, 1_000);
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(CASHOUT_REQUEST_ID))
				.willReturn(Optional.of(request));

		assertThatThrownBy(() -> pointCashoutRequestService.approve(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, PARENT_ID, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CASHOUT_REQUEST_NOT_FOUND);
	}

	@Test
	@DisplayName("이미 처리된 요청은 다시 승인할 수 없다")
	void 이미_처리된_요청은_다시_승인할_수_없다() {
		PointCashoutRequest request = PointCashoutRequest.pending(FAMILY_ID, CHILD_ID, PARENT_ID, 1_000);
		request.succeed();
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(CASHOUT_REQUEST_ID))
				.willReturn(Optional.of(request));

		assertThatThrownBy(() -> pointCashoutRequestService.approve(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, PARENT_ID, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CASHOUT_REQUEST_ALREADY_PROCESSED);

		verify(bankTransferClient, never()).transferFromParentToChild(any(), any(), anyInt(), any());
	}

	@Test
	@DisplayName("자녀가 자기 PENDING 요청을 취소하면 환급하고 CANCELLED 가 된다")
	void 자녀가_취소하면_환급하고_CANCELLED가_된다() {
		PointCashoutRequest request = PointCashoutRequest.pending(FAMILY_ID, CHILD_ID, PARENT_ID, 1_000);
		given(pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(CASHOUT_REQUEST_ID))
				.willReturn(Optional.of(request));

		PointCashoutRequestResult result = pointCashoutRequestService.cancel(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, CHILD_ID, KEY);

		assertThat(result.status()).isEqualTo(PointCashoutRequestStatus.CANCELLED);
		verify(pointDeductionService).refund(eq(CHILD_ID), eq(1_000), any(), any());
	}

	@Test
	@DisplayName("본인의 요청이 아니면 취소할 수 없다")
	void 본인_요청이_아니면_취소할_수_없다() {
		assertThatThrownBy(() -> pointCashoutRequestService.cancel(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, 999, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

		verify(pointCashoutRequestRepository, never()).findByCashoutRequestIdForUpdate(any());
	}

	@Test
	@DisplayName("이미 처리된 요청은 취소할 수 없다")
	void 이미_처리된_요청은_취소할_수_없다() {
		PointCashoutRequest request = PointCashoutRequest.pending(FAMILY_ID, CHILD_ID, PARENT_ID, 1_000);
		request.succeed();
		given(pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(CASHOUT_REQUEST_ID))
				.willReturn(Optional.of(request));

		assertThatThrownBy(() -> pointCashoutRequestService.cancel(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, CHILD_ID, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CASHOUT_REQUEST_ALREADY_PROCESSED);

		verify(pointDeductionService, never()).refund(any(), anyInt(), any(), any());
	}

	@Test
	@DisplayName("부모가 거절하면 환급하고 DECLINED 가 된다")
	void 부모가_거절하면_환급하고_DECLINED가_된다() {
		PointCashoutRequest request = PointCashoutRequest.pending(FAMILY_ID, CHILD_ID, PARENT_ID, 1_000);
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(CASHOUT_REQUEST_ID))
				.willReturn(Optional.of(request));

		PointCashoutRequestResult result = pointCashoutRequestService.decline(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, PARENT_ID, KEY);

		assertThat(result.status()).isEqualTo(PointCashoutRequestStatus.DECLINED);
		verify(pointDeductionService).refund(eq(CHILD_ID), eq(1_000), any(), any());
		verify(bankTransferClient, never()).transferFromParentToChild(any(), any(), anyInt(), any());
	}

	@Test
	@DisplayName("해당 가족의 부모가 아니면 거절할 수 없다")
	void 가족의_부모가_아니면_거절할_수_없다() {
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(false);

		assertThatThrownBy(() -> pointCashoutRequestService.decline(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, PARENT_ID, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

		verify(pointCashoutRequestRepository, never()).findByCashoutRequestIdForUpdate(any());
	}

	@Test
	@DisplayName("이미 처리된 요청은 거절할 수 없다")
	void 이미_처리된_요청은_거절할_수_없다() {
		PointCashoutRequest request = PointCashoutRequest.pending(FAMILY_ID, CHILD_ID, PARENT_ID, 1_000);
		request.cancel();
		given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(CASHOUT_REQUEST_ID))
				.willReturn(Optional.of(request));

		assertThatThrownBy(() -> pointCashoutRequestService.decline(
				CASHOUT_REQUEST_ID, FAMILY_ID, CHILD_ID, PARENT_ID, KEY))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CASHOUT_REQUEST_ALREADY_PROCESSED);

		verify(pointDeductionService, never()).refund(any(), anyInt(), any(), any());
	}

	@Test
	@DisplayName("목록 조회는 읽기 권한 판정을 그대로 쓴다")
	void 목록_조회는_읽기_권한을_확인한다() {
		given(pointCashoutRequestRepository.findByFamilyIdAndChildIdOrderByRequestedAtDesc(FAMILY_ID, CHILD_ID))
				.willReturn(java.util.List.of());

		pointCashoutRequestService.list(FAMILY_ID, CHILD_ID, CHILD_ID, null);

		verify(pointWalletQueryService).validateReadAccess(FAMILY_ID, CHILD_ID, CHILD_ID);
	}
}
