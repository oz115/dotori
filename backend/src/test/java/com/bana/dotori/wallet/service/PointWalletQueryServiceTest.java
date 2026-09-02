package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.response.PointBalanceResponse;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;
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
