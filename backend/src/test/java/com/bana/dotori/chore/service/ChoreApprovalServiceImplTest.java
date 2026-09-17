package com.bana.dotori.chore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.chore.dto.ChoreApprovalResponse;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreCompletionRequest;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;
import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.mapper.ChoreCompletionMapper;
import com.bana.dotori.chore.repository.ChoreCompletionRequestRepository;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointWalletService;

/**
 * 2계층 - 승인과 보상 지급 규칙만 검증한다 (CHORE-09 + CHORE-07).
 *
 * <p>가족·아이 경계는 {@link ChoreAccessValidator}, 실제 잔액 변경은
 * {@link PointWalletService} 몫이라 둘 다 모킹한다. 여기서 보는 것은
 * "무엇을 막고, 지갑에 무엇을 넘기는가" 다.
 *
 * <p>동시에 두 건을 승인했을 때 한 건만 지급되는지는 DB 유니크 제약이 하는 일이라
 * 여기서 검증할 수 없다 — S15P21A505-132 동시성 테스트 몫이다.
 */
@DisplayName("ChoreApprovalServiceImpl 승인과 보상 지급")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChoreApprovalServiceImplTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private static final Integer FAMILY_ID = 10;
	private static final Integer PARENT_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final Integer SIBLING_ID = 3;
	private static final Integer CHORE_ID = 7;
	private static final Integer REQUEST_ID = 100;
	private static final int REWARD_POINTS = 500;
	private static final LocalDate PERIOD_START = LocalDate.of(2026, 9, 9);

	@Mock
	private ChoreRepository choreRepository;

	@Mock
	private ChoreCompletionRequestRepository completionRequestRepository;

	@Mock
	private ChoreAccessValidator accessValidator;

	@Mock
	private PointWalletService pointWalletService;

	@Spy
	private ChoreCompletionMapper completionMapper;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ChoreApprovalServiceImpl choreApprovalService;

	private Chore chore(Integer rewardPoints, LocalDate startDate, LocalDate endDate) {
		Chore chore = Chore.builder()
			.familyId(FAMILY_ID)
			.parentId(PARENT_ID)
			.childId(CHILD_ID)
			.title("설거지")
			.difficulty(ChoreDifficulty.NORMAL)
			.rewardPoints(rewardPoints)
			.repeatType(ChoreRepeatType.DAY)
			.startDate(startDate)
			.endDate(endDate)
			.build();
		ReflectionTestUtils.setField(chore, "id", CHORE_ID);
		return chore;
	}

	private ChoreCompletionRequest pendingRequest(Integer ownerId) {
		ChoreCompletionRequest request = ChoreCompletionRequest.builder()
			.choreId(CHORE_ID)
			.childId(ownerId)
			.build();
		ReflectionTestUtils.setField(request, "id", REQUEST_ID);
		return request;
	}

	private PointChangeResult paid(boolean replayed) {
		return new PointChangeResult(1, CHILD_ID, PointTransactionReason.CHORE_REWARD, REWARD_POINTS,
			REWARD_POINTS, OffsetDateTime.now(KST), replayed);
	}

	private void givenApprovable(ChoreCompletionRequest request, Chore chore, boolean replayed) {
		given(completionRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.of(request));
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(chore));
		given(pointWalletService.deposit(any())).willReturn(paid(replayed));
	}

	private ChoreApprovalResponse approve() {
		return choreApprovalService.approve(PARENT_ID, FAMILY_ID, CHILD_ID, REQUEST_ID);
	}

	@Nested
	@DisplayName("승인이 성공하면")
	class Approved {

		@Test
		@DisplayName("SUCCEED 로 바뀌고 지급액을 응답한다")
		void 정상_승인() {
			ChoreCompletionRequest request = pendingRequest(CHILD_ID);
			givenApprovable(request, chore(REWARD_POINTS, PERIOD_START, PERIOD_START), false);

			ChoreApprovalResponse response = approve();

			assertThat(response.choreCompletionRequestId()).isEqualTo(REQUEST_ID);
			assertThat(response.status()).isEqualTo(ChoreCompletionStatus.SUCCEED);
			assertThat(response.rewardPoints()).isEqualTo(REWARD_POINTS);
			assertThat(request.getStatus()).isEqualTo(ChoreCompletionStatus.SUCCEED);
			// 응답의 처리 시각은 엔티티에 기록된 값이어야 한다. 서비스가 따로 계산한 값을
			// 실어 보내면 DB 에 저장된 시각과 응답이 어긋날 수 있다.
			assertThat(response.processedAt()).isNotNull().isEqualTo(request.getProcessedAt());
		}

		@Test
		@DisplayName("승인 시점의 할 일 보상액을 지급한다")
		void 승인_시점_금액을_지급한다() {
			givenApprovable(pendingRequest(CHILD_ID), chore(1200, PERIOD_START, PERIOD_START), false);

			approve();

			ArgumentCaptor<PointChangeCommand> captor = ArgumentCaptor.forClass(PointChangeCommand.class);
			verify(pointWalletService).deposit(captor.capture());
			// 요청 시점 값을 스냅샷하지 않으므로 대기 중에 부모가 고친 금액이 나간다.
			assertThat(captor.getValue().amount()).isEqualTo(1200);
			assertThat(captor.getValue().childId()).isEqualTo(CHILD_ID);
			assertThat(captor.getValue().reason()).isEqualTo(PointTransactionReason.CHORE_REWARD);
		}

		@Test
		@DisplayName("멱등키를 주기에서 만들어 넘긴다")
		void 멱등키는_주기에서_나온다() {
			givenApprovable(pendingRequest(CHILD_ID), chore(REWARD_POINTS, PERIOD_START, PERIOD_START), false);

			approve();

			ArgumentCaptor<PointChangeCommand> captor = ArgumentCaptor.forClass(PointChangeCommand.class);
			verify(pointWalletService).deposit(captor.capture());
			// 이 키가 point_transactions 의 유니크 제약에 걸려 주기당 1회를 보장한다.
			assertThat(captor.getValue().idempotencyKey()).isEqualTo("chore:" + CHORE_ID + ":" + PERIOD_START);
		}

		@Test
		@DisplayName("지갑이 없으면 먼저 만든다")
		void 지갑을_먼저_만든다() {
			givenApprovable(pendingRequest(CHILD_ID), chore(REWARD_POINTS, PERIOD_START, PERIOD_START), false);

			approve();

			// 집안일 보상이 그 아이의 첫 포인트일 수 있다. 없으면 deposit 이 WALLET_NOT_FOUND 로 터진다.
			verify(pointWalletService).createWalletIfAbsent(CHILD_ID);
		}

		@Test
		@DisplayName("기간이 지난 할 일도 승인할 수 있다")
		void 기간이_지나도_승인된다() {
			LocalDate longAgo = LocalDate.now(KST).minusMonths(2);
			givenApprovable(pendingRequest(CHILD_ID), chore(REWARD_POINTS, longAgo, longAgo), false);

			// 막는 것은 새 요청뿐이다. 이미 올라온 인증은 부모가 처리할 수 있어야 한다.
			assertThat(approve().status()).isEqualTo(ChoreCompletionStatus.SUCCEED);
		}

		@Test
		@DisplayName("지워진 할 일의 요청도 승인할 수 있다")
		void 지워진_할_일도_승인된다() {
			Chore deleted = chore(REWARD_POINTS, PERIOD_START, PERIOD_START);
			deleted.softDelete();
			givenApprovable(pendingRequest(CHILD_ID), deleted, false);

			assertThat(approve().status()).isEqualTo(ChoreCompletionStatus.SUCCEED);
		}
	}

	@Nested
	@DisplayName("승인을 막을 때")
	class Rejected {

		@Test
		@DisplayName("부모가 아니면 403이고 지급하지 않는다")
		void 부모만_승인할_수_있다() {
			willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(accessValidator).requireParent(PARENT_ID);

			assertThatThrownBy(ChoreApprovalServiceImplTest.this::approve)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

			verify(pointWalletService, never()).deposit(any());
		}

		@Test
		@DisplayName("없는 요청이면 404다")
		void 없는_요청() {
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.empty());

			assertThatThrownBy(ChoreApprovalServiceImplTest.this::approve)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND);
		}

		@Test
		@DisplayName("경로의 아이와 요청의 주인이 다르면 404다")
		void 남의_요청() {
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID))
				.willReturn(Optional.of(pendingRequest(SIBLING_ID)));

			// 403 을 주면 그 requestId 가 실재한다는 사실이 새어 나간다.
			assertThatThrownBy(ChoreApprovalServiceImplTest.this::approve)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND);

			verify(pointWalletService, never()).deposit(any());
		}

		@Test
		@DisplayName("요청이 가리키는 할 일이 다른 가족이면 404다")
		void 다른_가족의_할_일() {
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID))
				.willReturn(Optional.of(pendingRequest(CHILD_ID)));
			given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.empty());

			assertThatThrownBy(ChoreApprovalServiceImplTest.this::approve)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_NOT_FOUND);
		}

		@Test
		@DisplayName("이미 처리된 요청이면 409이고 지급하지 않는다")
		void 이미_처리된_요청() {
			ChoreCompletionRequest approved = pendingRequest(CHILD_ID);
			approved.approve(OffsetDateTime.now(KST));
			givenApprovable(approved, chore(REWARD_POINTS, PERIOD_START, PERIOD_START), false);

			// 부모가 승인 버튼을 두 번 눌렀을 때 지갑 락을 잡기 전에 걸러야 한다.
			assertThatThrownBy(ChoreApprovalServiceImplTest.this::approve)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS);

			verify(pointWalletService, never()).deposit(any());
		}

		@Test
		@DisplayName("보상이 산정되지 않았으면 400이고 상태를 바꾸지 않는다")
		void 보상_미산정() {
			ChoreCompletionRequest request = pendingRequest(CHILD_ID);
			givenApprovable(request, chore(null, PERIOD_START, PERIOD_START), false);

			assertThatThrownBy(ChoreApprovalServiceImplTest.this::approve)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_NOT_EVALUATED);

			assertThat(request.getStatus()).isEqualTo(ChoreCompletionStatus.PENDING);
			verify(pointWalletService, never()).deposit(any());
		}

		@Test
		@DisplayName("이번 주기에 이미 지급됐으면 409로 되돌린다")
		void 멱등키에_걸리면_409() {
			givenApprovable(pendingRequest(CHILD_ID), chore(REWARD_POINTS, PERIOD_START, PERIOD_START), true);

			// 지갑이 이전 거래를 그대로 돌려준 경우다(replayed). 여기서 던지지 않으면
			// 지급 없이 SUCCEED 로 바뀐 요청이 남는다.
			assertThatThrownBy(ChoreApprovalServiceImplTest.this::approve)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_REWARDED);
		}
	}
}
