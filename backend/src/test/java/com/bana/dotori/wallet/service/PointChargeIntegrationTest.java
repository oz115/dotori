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
import com.bana.dotori.mock.bank.MockBankTransferClient;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointWalletRepository;
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
	@DisplayName("충전에 성공하면 포인트가 늘고 계좌에서 같은 금액이 빠진다")
	void 충전_성공() {
		mockBankTransferClient.registerAccount(childId, "김도토", 500_000);

		PointChangeResult result = pointChargeService.charge(familyId, childId, 10_000, "charge-success");

		assertThat(result.balanceAfter()).isEqualTo(15_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(15_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
		assertThat(mockBankTransferClient.findAccount(childId).balance())
				.as("원과 포인트는 1:1 이라 계좌에서 정확히 같은 금액이 빠져야 한다")
				.isEqualTo(490_000);
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
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(15_000);
	}

	@Test
	@DisplayName("같은 멱등키로 다시 충전하면 포인트도 송금도 한 번만 일어난다")
	void 중복_충전은_한번만_반영된다() {
		mockBankTransferClient.registerAccount(childId, "김도토", 500_000);

		pointChargeService.charge(familyId, childId, 10_000, "charge-once");
		PointChangeResult second = pointChargeService.charge(familyId, childId, 10_000, "charge-once");

		assertThat(second.replayed()).isTrue();
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(15_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
		assertThat(mockBankTransferClient.findAccount(childId).balance())
				.as("중복 요청으로 돈이 두 번 빠져나가면 안 된다")
				.isEqualTo(490_000);
	}
}
