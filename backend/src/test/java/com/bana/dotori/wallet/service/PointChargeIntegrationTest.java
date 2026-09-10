package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.support.MockBankTransferClient;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * 통합 - 충전 한 건이 실제 DB 와 송금 목업을 거쳐 끝까지 도는지 본다.
 *
 * <p>테스트 메서드에 {@code @Transactional} 을 붙이지 않는다.
 * 테스트가 통째로 롤백되면 "서비스가 롤백했는지"를 구분할 수 없기 때문이다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("포인트 충전 통합")
class PointChargeIntegrationTest {


	@Autowired
	private PointChargeService pointChargeService;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private MockBankTransferClient mockBankTransferClient;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer familyId;
	private Integer childId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		mockBankTransferClient.clear();
		Integer parentId = WalletTestFixture.insertParent(jdbcTemplate, "parent-charge");
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-charge");
		familyId = WalletTestFixture.insertFamilyWithChild(jdbcTemplate, parentId, childId);
		pointWalletRepository.save(PointWallet.openWithBalance(childId, 5_000));
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		mockBankTransferClient.clear();
	}

	@Test
	@DisplayName("충전에 성공하면 현금의 10분의 1 만큼 포인트가 늘고 계좌에서는 현금 전액이 빠진다")
	void 충전_성공() {
		mockBankTransferClient.registerAccount(childId, "김도토", 500_000);

		PointChangeResult result = pointChargeService.charge(familyId, childId, 10_000, "charge-success");

		assertThat(result.balanceAfter()).as("5,000 + 10,000원/10 = 6,000").isEqualTo(6_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(6_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
		assertThat(mockBankTransferClient.findAccount(childId).balance())
				.as("포인트는 10분의 1이지만 계좌에서는 요청한 현금 전액이 빠져야 한다")
				.isEqualTo(490_000);
	}

	@Test
	@DisplayName("지갑이 없던 아이도 첫 충전으로 지갑이 만들어지고 포인트가 들어온다")
	void 첫_충전이면_지갑이_만들어진다() {
		Integer newChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-no-wallet");
		WalletTestFixture.addFamilyMember(jdbcTemplate, familyId, newChildId);
		mockBankTransferClient.registerAccount(newChildId, "새 아이", 500_000);

		// 가입 직후 아이는 point_wallets 행이 없다. 여기가 깨지면 WALLET_NOT_FOUND 가 난다.
		PointChangeResult result = pointChargeService.charge(familyId, newChildId, 10_000, "charge-first");

		assertThat(result.balanceAfter()).isEqualTo(1_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, newChildId)).isEqualTo(1_000);
	}

	@Test
	@DisplayName("10원 단위가 아니면 지갑도 원장도 계좌도 그대로다")
	void 십원_단위가_아니면_거부된다() {
		mockBankTransferClient.registerAccount(childId, "김도토", 500_000);

		assertThatThrownBy(() -> pointChargeService.charge(familyId, childId, 1_005, "charge-odd"))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CHARGE_UNIT);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(5_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
		assertThat(mockBankTransferClient.findAccount(childId).balance()).isEqualTo(500_000);
	}

	@Test
	@DisplayName("다른 아이가 같은 멱등키를 써도 각자 정상 충전된다")
	void 멱등키는_지갑별로_독립이다() {
		Integer otherChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-same-key");
		WalletTestFixture.addFamilyMember(jdbcTemplate, familyId, otherChildId);
		pointWalletRepository.save(PointWallet.openWithBalance(otherChildId, 0));
		mockBankTransferClient.registerAccount(childId, "김도토", 500_000);
		mockBankTransferClient.registerAccount(otherChildId, "이도토", 500_000);

		// 멱등키는 클라이언트가 정하는 값이라 사용자끼리 겹칠 수 있다.
		// 전역 유일이던 시절에는 뒤에 온 요청이 앞 사람의 거래를 자기 것으로 착각했다.
		String sharedKey = "charge-collision";
		PointChangeResult first = pointChargeService.charge(familyId, childId, 10_000, sharedKey);
		PointChangeResult second = pointChargeService.charge(familyId, otherChildId, 20_000, sharedKey);

		assertThat(second.replayed()).as("남의 거래를 재사용하면 안 된다").isFalse();
		assertThat(first.balanceAfter()).isEqualTo(6_000);
		assertThat(second.balanceAfter()).isEqualTo(2_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(6_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, otherChildId)).isEqualTo(2_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(2);
	}

	@Test
	@DisplayName("계좌 잔액이 부족하면 포인트가 발행되지 않고 원장도 비어 있다")
	void 계좌_잔액_부족시_포인트가_발행되지_않는다() {
		mockBankTransferClient.registerAccount(childId, "김도토", 5_000);

		assertThatThrownBy(() -> pointChargeService.charge(familyId, childId, 10_000, "charge-poor"))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_ACCOUNT_BALANCE);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId))
				.as("송금이 실패했으면 포인트는 그대로여야 한다")
				.isEqualTo(5_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate))
				.as("롤백됐으므로 원장에도 아무것도 남지 않아야 한다")
				.isZero();
	}

	@Test
	@DisplayName("송금 실패 후 재시도하면 같은 멱등키로도 정상 충전된다")
	void 실패한_요청은_재시도할_수_있다() {
		mockBankTransferClient.registerAccount(childId, "김도토", 5_000);
		assertThatThrownBy(() -> pointChargeService.charge(familyId, childId, 10_000, "charge-retry"))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_ACCOUNT_BALANCE);

		// 롤백으로 멱등키 행도 사라졌으므로 같은 키를 다시 쓸 수 있어야 한다.
		mockBankTransferClient.registerAccount(childId, "김도토", 500_000);
		PointChangeResult retried = pointChargeService.charge(familyId, childId, 10_000, "charge-retry");

		assertThat(retried.replayed()).isFalse();
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(6_000);
	}

	@Test
	@DisplayName("같은 멱등키로 다시 충전하면 포인트도 송금도 한 번만 일어난다")
	void 중복_충전은_한번만_반영된다() {
		mockBankTransferClient.registerAccount(childId, "김도토", 500_000);

		pointChargeService.charge(familyId, childId, 10_000, "charge-once");
		PointChangeResult second = pointChargeService.charge(familyId, childId, 10_000, "charge-once");

		assertThat(second.replayed()).isTrue();
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(6_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
		assertThat(mockBankTransferClient.findAccount(childId).balance())
				.as("중복 요청으로 돈이 두 번 빠져나가면 안 된다")
				.isEqualTo(490_000);
	}
}
