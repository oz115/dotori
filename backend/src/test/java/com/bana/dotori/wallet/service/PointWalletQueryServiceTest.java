package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.request.TransactionCursor;
import com.bana.dotori.wallet.dto.response.PointBalanceResponse;
import com.bana.dotori.wallet.dto.response.PointTransactionHistoryResponse;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;
import com.bana.dotori.wallet.repository.PointTransactionRepository;
import com.bana.dotori.wallet.repository.PointWalletRepository;

/**
 * 서비스 계층 - 조회 권한 판정과 응답 조립을 검증한다.
 *
 * <p>여기가 실패하면 원인은 HTTP 매핑도 DB 도 아니고 "누가 어떤 지갑을 볼 수 있는가",
 * 그리고 "커서를 어떻게 자르고 만드는가"다.
 */
@DisplayName("PointWalletQueryService 조회 흐름")
@ExtendWith(MockitoExtension.class)
class PointWalletQueryServiceTest {

	private static final Integer FAMILY_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final Integer PARENT_ID = 9;
	private static final Integer OTHER_CHILD_ID = 3;

	@Mock
	private PointWalletRepository pointWalletRepository;

	@Mock
	private PointTransactionRepository pointTransactionRepository;

	@Mock
	private FamilyMembershipReader familyMembershipReader;

	@InjectMocks
	private PointWalletQueryService pointWalletQueryService;

	@Nested
	@DisplayName("잔액을 조회할 때")
	class GetBalance {

		@Test
		@DisplayName("아이 본인은 자기 잔액을 조회한다")
		void 본인_조회() {
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
			given(pointWalletRepository.findByChildId(CHILD_ID))
					.willReturn(Optional.of(PointWallet.openWithBalance(CHILD_ID, 12_000)));

			PointBalanceResponse response =
					pointWalletQueryService.getBalance(FAMILY_ID, CHILD_ID, CHILD_ID);

			assertThat(response.childId()).isEqualTo(CHILD_ID);
			assertThat(response.balance()).isEqualTo(12_000);
			assertThat(response.updatedAt()).isNotNull();
		}

		@Test
		@DisplayName("같은 가족 부모는 자녀 잔액을 조회한다")
		void 부모가_자녀를_조회한다() {
			given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
			given(pointWalletRepository.findByChildId(CHILD_ID))
					.willReturn(Optional.of(PointWallet.openWithBalance(CHILD_ID, 8_000)));

			PointBalanceResponse response =
					pointWalletQueryService.getBalance(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.balance()).isEqualTo(8_000);
		}

		@Test
		@DisplayName("지갑이 아직 없으면 오류 대신 잔액 0을 돌려준다")
		void 지갑이_없으면_0이다() {
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
			given(pointWalletRepository.findByChildId(CHILD_ID)).willReturn(Optional.empty());

			PointBalanceResponse response =
					pointWalletQueryService.getBalance(FAMILY_ID, CHILD_ID, CHILD_ID);

			// 404 를 던지면 가입 직후 화면이 에러로 뜬다. 잔액이 없는 것은 오류가 아니다.
			assertThat(response.balance()).isZero();
			assertThat(response.childId()).isEqualTo(CHILD_ID);
			assertThat(response.updatedAt())
					.as("지갑이 없으면 마지막 변경 시각도 없다")
					.isNull();
		}
	}

	@Nested
	@DisplayName("거래내역을 조회할 때")
	class GetTransactions {


		@BeforeEach
		void 본인_조회를_허용한다() {
			PointWallet wallet = PointWallet.openWithBalance(CHILD_ID, 6_000);
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
			given(pointWalletRepository.findByChildId(CHILD_ID)).willReturn(Optional.of(wallet));
		}

		@Test
		@DisplayName("기간·사유·정렬 조건을 조회 조건으로 그대로 넘긴다")
		void 조건을_그대로_넘긴다() {
			givenFound(List.of());

			pointWalletQueryService.getTransactions(FAMILY_ID, CHILD_ID, CHILD_ID,
					LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3),
					PointTransactionReason.CHARGE, Sort.Direction.ASC, null, 20);

			ArgumentCaptor<OffsetDateTime> startAt = ArgumentCaptor.forClass(OffsetDateTime.class);
			ArgumentCaptor<OffsetDateTime> endAt = ArgumentCaptor.forClass(OffsetDateTime.class);
			verify(pointTransactionRepository).search(eq(CHILD_ID), startAt.capture(), endAt.capture(),
					eq(PointTransactionReason.CHARGE), isNull(), eq(Sort.Direction.ASC), anyInt());

			assertThat(startAt.getValue())
					.as("시작일은 그 날 0시(KST)부터")
					.isEqualTo(OffsetDateTime.parse("2026-09-01T00:00+09:00"));
			assertThat(endAt.getValue())
					.as("종료일을 포함하려면 반개구간의 끝이 다음 날 0시여야 한다")
					.isEqualTo(OffsetDateTime.parse("2026-09-04T00:00+09:00"));
		}

		@Test
		@DisplayName("다음이 있는지 보려고 한 건을 더 요청한다")
		void 한_건을_더_요청한다() {
			givenFound(List.of());

			pointWalletQueryService.getTransactions(FAMILY_ID, CHILD_ID, CHILD_ID,
					null, null, null, null, null, 20);

			// COUNT(*) 를 따로 실행하지 않으려는 방법이다. limit 이 size 와 같으면
			// 마지막 묶음에서도 다음이 있는지 알 수 없다.
			verify(pointTransactionRepository).search(any(), any(), any(), any(), any(), any(), eq(21));
		}

		@Test
		@DisplayName("조건이 없으면 기간·사유 없이 최신순으로 조회한다")
		void 조건이_없으면_전체_최신순이다() {
			givenFound(List.of());

			pointWalletQueryService.getTransactions(FAMILY_ID, CHILD_ID, CHILD_ID,
					null, null, null, null, null, 20);

			verify(pointTransactionRepository).search(eq(CHILD_ID), isNull(), isNull(), isNull(),
					isNull(), eq(Sort.Direction.DESC), anyInt());
		}

		@Test
		@DisplayName("가져온 것이 size 이하면 마지막 묶음으로 보고 커서를 주지 않는다")
		void 마지막_묶음은_커서가_없다() {
			givenFound(List.of(transaction(1, PointTransactionReason.CHARGE, 1_000, 6_000)));

			PointTransactionHistoryResponse response = pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, null, 20);

			assertThat(response.page().items()).hasSize(1);
			assertThat(response.page().hasNext()).isFalse();
			assertThat(response.page().nextCursor())
					.as("더 없으면 커서를 주지 않아야 클라이언트가 스크롤을 멈춘다")
					.isNull();
		}

		@Test
		@DisplayName("여분이 있으면 잘라내고, 커서는 돌려주는 목록의 마지막으로 만든다")
		void 여분을_잘라내고_커서를_만든다() {
			// size=2 로 요청하면 서비스는 3건을 가져온다. 여분 1건은 응답에서 빠져야 하고,
			// 커서를 여분으로 만들면 그 거래가 다음 요청에서 통째로 빠진다.
			PointTransaction first = transaction(1, PointTransactionReason.CHARGE, 1_000, 6_000);
			PointTransaction second = transaction(2, PointTransactionReason.GRANT, 3_000, 9_000);
			PointTransaction spare = transaction(3, PointTransactionReason.PURCHASE, 500, 8_500);
			givenFound(List.of(first, second, spare));

			PointTransactionHistoryResponse response = pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, null, 2);

			assertThat(response.page().items()).hasSize(2);
			assertThat(response.page().hasNext()).isTrue();
			assertThat(TransactionCursor.decode(response.page().nextCursor()))
					.as("커서는 여분이 아니라 돌려준 목록의 마지막이어야 한다")
					.isEqualTo(new TransactionCursor(second.getCreatedAt(), 2));
		}

		@Test
		@DisplayName("클라이언트가 준 커서를 해석해 저장소로 넘긴다")
		void 커서를_해석해_넘긴다() {
			OffsetDateTime at = OffsetDateTime.parse("2026-09-02T10:00:00+09:00");
			String encoded = new TransactionCursor(at, 42).encode();
			givenFound(List.of());

			pointWalletQueryService.getTransactions(FAMILY_ID, CHILD_ID, CHILD_ID,
					null, null, null, null, encoded, 20);

			verify(pointTransactionRepository).search(any(), any(), any(), any(),
					eq(new TransactionCursor(at, 42)), any(), anyInt());
		}

		@Test
		@DisplayName("거래가 없으면 오류 대신 빈 목록과 현재 잔액을 돌려준다")
		void 거래가_없으면_빈_목록이다() {
			givenFound(List.of());

			PointTransactionHistoryResponse response = pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, null, 20);

			assertThat(response.page().items()).isEmpty();
			assertThat(response.page().hasNext()).isFalse();
			assertThat(response.balance()).as("목록이 비어도 잔액은 알려줘야 한다").isEqualTo(6_000);
		}

		@Test
		@DisplayName("거래를 응답 항목으로 옮기고 입출금 방향을 채운다")
		void 응답_항목을_조립한다() {
			givenFound(List.of(
					transaction(1, PointTransactionReason.CHARGE, 1_000, 6_000),
					transaction(2, PointTransactionReason.PURCHASE, 500, 5_500)));

			PointTransactionHistoryResponse response = pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, null, 20);

			assertThat(response.page().items()).hasSize(2);
			assertThat(response.page().items().get(0).transactionType())
					.isEqualTo(PointTransactionReason.CHARGE);
			assertThat(response.page().items().get(0).transactionDirection()).isEqualTo("IN");
			assertThat(response.page().items().get(1).transactionDirection()).isEqualTo("OUT");
		}

		@Test
		@DisplayName("같은 날을 시작·종료로 주면 그 하루가 조회된다")
		void 하루만_조회할_수_있다() {
			givenFound(List.of());

			pointWalletQueryService.getTransactions(FAMILY_ID, CHILD_ID, CHILD_ID,
					LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2), null, null, null, 20);

			ArgumentCaptor<OffsetDateTime> startAt = ArgumentCaptor.forClass(OffsetDateTime.class);
			ArgumentCaptor<OffsetDateTime> endAt = ArgumentCaptor.forClass(OffsetDateTime.class);
			verify(pointTransactionRepository).search(any(), startAt.capture(), endAt.capture(),
					any(), any(), any(), anyInt());

			assertThat(startAt.getValue()).isEqualTo(OffsetDateTime.parse("2026-09-02T00:00+09:00"));
			assertThat(endAt.getValue()).isEqualTo(OffsetDateTime.parse("2026-09-03T00:00+09:00"));
		}

		private void givenFound(List<PointTransaction> found) {
			given(pointTransactionRepository.search(any(), any(), any(), any(), any(), any(), anyInt()))
					.willReturn(found);
		}

		private PointTransaction transaction(int id, PointTransactionReason reason,
				int amount, int balanceAfter) {
			PointTransaction transaction = PointTransaction.record(
					CHILD_ID, reason, amount, balanceAfter, reason.name(), "key-" + id);
			ReflectionTestUtils.setField(transaction, "pointTransactionId", id);
			return transaction;
		}
	}

	@Nested
	@DisplayName("거래내역 조회 요청이 잘못됐을 때")
	class InvalidTransactionRequest {

		@BeforeEach
		void 본인_조회를_허용한다() {
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		}

		@Test
		@DisplayName("시작일이 종료일보다 늦으면 조회 전에 거부한다")
		void 기간이_뒤집히면_거부한다() {
			assertThatThrownBy(() -> pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID,
					LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 1), null, null, null, 20))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);

			verify(pointWalletRepository, never()).findByChildId(any());
		}

		@Test
		@DisplayName("커서가 깨졌으면 조회하지 않고 거부한다")
		void 깨진_커서를_거부한다() {
			// 조용히 첫 묶음으로 되돌리면 클라이언트가 같은 목록을 무한히 다시 받는다.
			assertThatThrownBy(() -> pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, "!!not-a-cursor!!", 20))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURSOR);

			verify(pointWalletRepository, never()).findByChildId(any());
		}

		@Test
		@DisplayName("조회 건수가 상한을 넘으면 거부한다")
		void 건수_상한을_넘기면_거부한다() {
			// 상한이 없으면 size 한 방으로 원장 전체를 끌어갈 수 있다.
			assertThatThrownBy(() -> pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, null,
					PointWalletQueryService.MAX_PAGE_SIZE + 1))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAGE_REQUEST);
		}

		@Test
		@DisplayName("조회 건수가 0 이하면 거부한다")
		void 건수가_0이하면_거부한다() {
			assertThatThrownBy(() -> pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, null, 0))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAGE_REQUEST);
		}
	}

	@Nested
	@DisplayName("지갑이 없을 때")
	class NoWallet {

		@Test
		@DisplayName("거래내역은 조회하지 않고 빈 목록과 잔액 0을 돌려준다")
		void 지갑이_없으면_조회하지_않는다() {
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
			given(pointWalletRepository.findByChildId(CHILD_ID)).willReturn(Optional.empty());

			PointTransactionHistoryResponse response = pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, null, 20);

			assertThat(response.balance()).isZero();
			assertThat(response.page().items()).isEmpty();
			assertThat(response.page().hasNext()).isFalse();
			verify(pointTransactionRepository, never())
					.search(any(), any(), any(), any(), any(), any(), anyInt());
		}
	}

	@Nested
	@DisplayName("거래내역 조회 권한")
	class TransactionAccess {

		@Test
		@DisplayName("잔액 조회와 같은 판정을 쓴다")
		void 잔액과_같은_판정이다() {
			// 잔액은 막혔는데 거래내역으로 같은 정보가 새어 나가면 안 된다.
			given(familyMembershipReader.isParentOfFamily(FAMILY_ID, CHILD_ID)).willReturn(false);

			assertThatThrownBy(() -> pointWalletQueryService.getTransactions(
					FAMILY_ID, OTHER_CHILD_ID, CHILD_ID, null, null, null, null, null, 20))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_ACCESS_DENIED);

			verify(pointWalletRepository, never()).findByChildId(any());
		}
	}

	@Nested
	@DisplayName("조회 권한을 판정할 때")
	class ValidateReadAccess {

		@Test
		@DisplayName("아이가 남의 지갑을 지목하면 거부한다")
		void 아이는_남의_지갑을_못_본다() {
			// 아이는 부모가 아니므로 isParentOfFamily 에서 걸린다.
			given(familyMembershipReader.isParentOfFamily(FAMILY_ID, CHILD_ID)).willReturn(false);

			assertThatThrownBy(() ->
					pointWalletQueryService.getBalance(FAMILY_ID, OTHER_CHILD_ID, CHILD_ID))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_ACCESS_DENIED);

			verify(pointWalletRepository, never()).findByChildId(OTHER_CHILD_ID);
		}

		@Test
		@DisplayName("다른 가족 부모가 지목하면 거부한다")
		void 다른_가족_부모는_못_본다() {
			given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(false);

			assertThatThrownBy(() ->
					pointWalletQueryService.getBalance(FAMILY_ID, CHILD_ID, PARENT_ID))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_ACCESS_DENIED);

			verify(pointWalletRepository, never()).findByChildId(CHILD_ID);
		}

		@Test
		@DisplayName("가족 부모라도 대상이 그 가족 자녀가 아니면 거부한다")
		void 대상이_가족_자녀가_아니면_거부한다() {
			given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(true);
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, OTHER_CHILD_ID)).willReturn(false);

			assertThatThrownBy(() ->
					pointWalletQueryService.getBalance(FAMILY_ID, OTHER_CHILD_ID, PARENT_ID))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_ACCESS_DENIED);
		}

		@Test
		@DisplayName("본인 조회여도 그 가족의 자녀가 아니면 거부한다")
		void 본인이라도_가족_소속을_확인한다() {
			// 경로의 familyId 는 클라이언트가 정하는 값이다. 확인하지 않으면
			// 남의 가족 번호를 끼워 넣어도 200 이 나가 가족 구성을 추측할 수 있다.
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(false);

			assertThatThrownBy(() ->
					pointWalletQueryService.getBalance(FAMILY_ID, CHILD_ID, CHILD_ID))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_ACCESS_DENIED);

			verify(pointWalletRepository, never()).findByChildId(CHILD_ID);
		}

		@Test
		@DisplayName("호출자를 알 수 없으면 거부한다")
		void 호출자가_없으면_거부한다() {
			// 인증 필터가 통과시켰는데 principal 이 비는 상황. 조회를 열어 두면 안 된다.
			assertThatThrownBy(() ->
					pointWalletQueryService.getBalance(FAMILY_ID, CHILD_ID, null))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_ACCESS_DENIED);
		}

		@Test
		@DisplayName("허용되는 경우에는 예외를 던지지 않는다")
		void 허용되면_통과한다() {
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);

			assertThatCode(() ->
					pointWalletQueryService.validateReadAccess(FAMILY_ID, CHILD_ID, CHILD_ID))
					.doesNotThrowAnyException();
		}
	}
}
