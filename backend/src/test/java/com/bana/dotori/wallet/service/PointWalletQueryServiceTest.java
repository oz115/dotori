package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.response.PointBalanceResponse;
import com.bana.dotori.wallet.dto.response.PointTransactionHistoryResponse;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;
import com.bana.dotori.wallet.repository.PointTransactionRepository;
import com.bana.dotori.wallet.repository.PointWalletRepository;

/**
 * 서비스 계층 - 조회 권한 판정과 잔액 응답 조립을 검증한다.
 *
 * <p>여기가 실패하면 원인은 HTTP 매핑도 DB 도 아니고 "누가 어떤 지갑을 볼 수 있는가"다.
 * 권한 규칙은 조회 기능의 핵심이라 조합을 하나씩 못 박아 둔다.
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
			given(pointWalletRepository.findByUserId(CHILD_ID))
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
			given(pointWalletRepository.findByUserId(CHILD_ID))
					.willReturn(Optional.of(PointWallet.openWithBalance(CHILD_ID, 8_000)));

			PointBalanceResponse response =
					pointWalletQueryService.getBalance(FAMILY_ID, CHILD_ID, PARENT_ID);

			assertThat(response.balance()).isEqualTo(8_000);
		}

		@Test
		@DisplayName("지갑이 아직 없으면 오류 대신 잔액 0을 돌려준다")
		void 지갑이_없으면_0이다() {
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
			given(pointWalletRepository.findByUserId(CHILD_ID)).willReturn(Optional.empty());

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

		private static final PointWallet WALLET = wallet();

		private static PointWallet wallet() {
			PointWallet w = PointWallet.openWithBalance(CHILD_ID, 6_000);
			ReflectionTestUtils.setField(w, "walletId", 10);
			return w;
		}

		@BeforeEach
		void allowSelfRead() {
			given(familyMembershipReader.isChildOfFamily(FAMILY_ID, CHILD_ID)).willReturn(true);
		}

		@Test
		@DisplayName("기간·사유·정렬 조건을 조회 조건으로 그대로 넘긴다")
		void 조건을_그대로_넘긴다() {
			given(pointWalletRepository.findByUserId(CHILD_ID)).willReturn(Optional.of(WALLET));
			given(pointTransactionRepository.search(any(), any(), any(), any(), any()))
					.willReturn(Page.empty());

			pointWalletQueryService.getTransactions(FAMILY_ID, CHILD_ID, CHILD_ID,
					LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3),
					PointTransactionReason.CHARGE, Sort.Direction.ASC, 0, 20);

			ArgumentCaptor<OffsetDateTime> startAt = ArgumentCaptor.forClass(OffsetDateTime.class);
			ArgumentCaptor<OffsetDateTime> endAt = ArgumentCaptor.forClass(OffsetDateTime.class);
			ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
			verify(pointTransactionRepository).search(eq(10), startAt.capture(), endAt.capture(),
					eq(PointTransactionReason.CHARGE), pageable.capture());

			assertThat(startAt.getValue())
					.as("시작일은 그 날 0시(KST)부터")
					.isEqualTo(OffsetDateTime.parse("2026-09-01T00:00+09:00"));
			assertThat(endAt.getValue())
					.as("종료일을 포함하려면 반개구간의 끝이 다음 날 0시여야 한다")
					.isEqualTo(OffsetDateTime.parse("2026-09-04T00:00+09:00"));

			Sort sort = pageable.getValue().getSort();
			assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
			assertThat(sort.getOrderFor("pointTransactionId"))
					.as("같은 시각 거래가 여럿이면 페이지 경계에서 누락·중복이 생긴다")
					.isNotNull();
		}

		@Test
		@DisplayName("조건이 없으면 기간·사유 없이 최신순으로 조회한다")
		void 조건이_없으면_전체_최신순이다() {
			given(pointWalletRepository.findByUserId(CHILD_ID)).willReturn(Optional.of(WALLET));
			given(pointTransactionRepository.search(any(), any(), any(), any(), any()))
					.willReturn(Page.empty());

			pointWalletQueryService.getTransactions(FAMILY_ID, CHILD_ID, CHILD_ID,
					null, null, null, null, 0, 20);

			ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
			verify(pointTransactionRepository).search(eq(10), isNull(), isNull(), isNull(), pageable.capture());

			assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
					.as("거래내역은 최신순이 기본이다")
					.isEqualTo(Sort.Direction.DESC);
		}

		@Test
		@DisplayName("거래가 없으면 오류 대신 빈 목록과 현재 잔액을 돌려준다")
		void 거래가_없으면_빈_목록이다() {
			given(pointWalletRepository.findByUserId(CHILD_ID)).willReturn(Optional.of(WALLET));
			given(pointTransactionRepository.search(any(), any(), any(), any(), any()))
					.willReturn(Page.empty(PageRequest.of(0, 20)));

			PointTransactionHistoryResponse response = pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, 0, 20);

			assertThat(response.transactions()).isEmpty();
			assertThat(response.balance()).as("목록이 비어도 잔액은 알려줘야 한다").isEqualTo(6_000);
			assertThat(response.totalElements()).isZero();
			assertThat(response.hasNext()).isFalse();
		}

		@Test
		@DisplayName("지갑이 없으면 조회하지 않고 빈 목록과 잔액 0을 돌려준다")
		void 지갑이_없으면_조회하지_않는다() {
			given(pointWalletRepository.findByUserId(CHILD_ID)).willReturn(Optional.empty());

			PointTransactionHistoryResponse response = pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, 0, 20);

			assertThat(response.balance()).isZero();
			assertThat(response.transactions()).isEmpty();
			verify(pointTransactionRepository, never()).search(any(), any(), any(), any(), any());
		}

		@Test
		@DisplayName("거래를 응답 항목으로 옮기고 입출금 방향을 채운다")
		void 응답_항목을_조립한다() {
			PointTransaction charge = PointTransaction.record(
					10, PointTransactionReason.CHARGE, 1_000, 6_000, "포인트 충전", "k1");
			PointTransaction purchase = PointTransaction.record(
					10, PointTransactionReason.PURCHASE, 500, 5_500, "간식 구매", "k2");
			given(pointWalletRepository.findByUserId(CHILD_ID)).willReturn(Optional.of(WALLET));
			given(pointTransactionRepository.search(any(), any(), any(), any(), any()))
					.willReturn(new PageImpl<>(List.of(charge, purchase), PageRequest.of(0, 20), 2));

			PointTransactionHistoryResponse response = pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, 0, 20);

			assertThat(response.transactions()).hasSize(2);
			assertThat(response.transactions().get(0).transactionType())
					.isEqualTo(PointTransactionReason.CHARGE);
			assertThat(response.transactions().get(0).transactionDirection()).isEqualTo("IN");
			assertThat(response.transactions().get(0).description()).isEqualTo("포인트 충전");
			assertThat(response.transactions().get(1).transactionDirection()).isEqualTo("OUT");
		}

		@Test
		@DisplayName("시작일이 종료일보다 늦으면 조회 전에 거부한다")
		void 기간이_뒤집히면_거부한다() {
			assertThatThrownBy(() -> pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID,
					LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 1), null, null, 0, 20))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);

			verify(pointWalletRepository, never()).findByUserId(any());
		}

		@Test
		@DisplayName("같은 날을 시작·종료로 주면 그 하루가 조회된다")
		void 하루만_조회할_수_있다() {
			given(pointWalletRepository.findByUserId(CHILD_ID)).willReturn(Optional.of(WALLET));
			given(pointTransactionRepository.search(any(), any(), any(), any(), any()))
					.willReturn(Page.empty());

			pointWalletQueryService.getTransactions(FAMILY_ID, CHILD_ID, CHILD_ID,
					LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2), null, null, 0, 20);

			ArgumentCaptor<OffsetDateTime> startAt = ArgumentCaptor.forClass(OffsetDateTime.class);
			ArgumentCaptor<OffsetDateTime> endAt = ArgumentCaptor.forClass(OffsetDateTime.class);
			verify(pointTransactionRepository).search(any(), startAt.capture(), endAt.capture(), any(), any());

			assertThat(startAt.getValue()).isEqualTo(OffsetDateTime.parse("2026-09-02T00:00+09:00"));
			assertThat(endAt.getValue()).isEqualTo(OffsetDateTime.parse("2026-09-03T00:00+09:00"));
		}

		@Test
		@DisplayName("페이지 크기가 상한을 넘으면 거부한다")
		void 페이지_크기_상한을_넘기면_거부한다() {
			// 상한이 없으면 size 한 방으로 원장 전체를 끌어갈 수 있다.
			assertThatThrownBy(() -> pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null,
					0, PointWalletQueryService.MAX_PAGE_SIZE + 1))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAGE_REQUEST);
		}

		@Test
		@DisplayName("페이지 번호가 음수이거나 크기가 0이면 거부한다")
		void 잘못된_페이지_요청을_거부한다() {
			assertThatThrownBy(() -> pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, -1, 20))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAGE_REQUEST);

			assertThatThrownBy(() -> pointWalletQueryService.getTransactions(
					FAMILY_ID, CHILD_ID, CHILD_ID, null, null, null, null, 0, 0))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAGE_REQUEST);
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
					FAMILY_ID, OTHER_CHILD_ID, CHILD_ID, null, null, null, null, 0, 20))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_ACCESS_DENIED);

			verify(pointWalletRepository, never()).findByUserId(any());
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

			verify(pointWalletRepository, never()).findByUserId(OTHER_CHILD_ID);
		}

		@Test
		@DisplayName("다른 가족 부모가 지목하면 거부한다")
		void 다른_가족_부모는_못_본다() {
			given(familyMembershipReader.isParentOfFamily(FAMILY_ID, PARENT_ID)).willReturn(false);

			assertThatThrownBy(() ->
					pointWalletQueryService.getBalance(FAMILY_ID, CHILD_ID, PARENT_ID))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_ACCESS_DENIED);

			verify(pointWalletRepository, never()).findByUserId(CHILD_ID);
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

			verify(pointWalletRepository, never()).findByUserId(CHILD_ID);
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
