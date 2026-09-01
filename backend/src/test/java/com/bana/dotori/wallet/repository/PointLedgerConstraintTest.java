package com.bana.dotori.wallet.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * DB 제약조건 계층 - 애플리케이션을 우회해도 잘못된 값이 들어가지 않는지 확인한다.
 *
 * <p>여기서는 일부러 엔티티와 서비스를 건너뛰고 네이티브 SQL 로 직접 찔러 본다.
 * 도메인 검증은 이미 다른 테스트가 보고 있고, 이 테스트가 보려는 것은
 * "배치 작업이나 손으로 실행한 SQL 이 원장을 망가뜨릴 수 있는가"이기 때문이다.
 *
 * <p>이 제약이 없으면 잔액이 음수인 지갑이나 금액이 0인 거래가 조용히 쌓일 수 있다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("포인트 원장 DB 제약조건")
class PointLedgerConstraintTest {

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer childId;
	private Integer walletId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-constraint");
		walletId = pointWalletRepository.save(PointWallet.openWithBalance(childId, 1_000)).getWalletId();
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
	}

	@Test
	@DisplayName("잔액을 음수로 바꾸려는 SQL 은 CHECK 제약이 막는다")
	void 잔액은_음수가_될_수_없다() {
		assertThatThrownBy(() -> jdbcTemplate.update(
				"UPDATE point_wallets SET balance = -1 WHERE wallet_id = ?", walletId))
				.as("애플리케이션 락이 뚫려도 DB 가 마지막으로 막아야 한다")
				.isInstanceOf(DataAccessException.class);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(1_000);
	}

	@Test
	@DisplayName("잔액 0 은 허용한다")
	void 잔액_0은_허용된다() {
		assertThatCode(() -> jdbcTemplate.update(
				"UPDATE point_wallets SET balance = 0 WHERE wallet_id = ?", walletId))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("금액이 0 이하인 거래는 원장에 넣을 수 없다")
	void 거래_금액은_양수여야_한다() {
		assertThatThrownBy(() -> insertTransaction(0, 1_000, "zero-amount"))
				.isInstanceOf(DataAccessException.class);

		assertThatThrownBy(() -> insertTransaction(-500, 1_000, "negative-amount"))
				.isInstanceOf(DataAccessException.class);

		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
	}

	@Test
	@DisplayName("같은 멱등키를 두 번 넣으려 하면 유니크 인덱스가 막는다")
	void 멱등키는_중복될_수_없다() {
		insertTransaction(500, 1_500, "same-key");

		assertThatThrownBy(() -> insertTransaction(500, 2_000, "same-key"))
				.isInstanceOf(DataAccessException.class);

		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
	}

	@Test
	@DisplayName("멱등키가 NULL 인 거래는 여러 건 넣을 수 있다")
	void 멱등키가_없으면_여러건_허용된다() {
		insertTransaction(100, 1_100, null);
		insertTransaction(100, 1_200, null);

		assertThat(WalletTestFixture.countTransactions(jdbcTemplate))
				.as("스케줄러 지급처럼 멱등키 없는 거래는 부분 유니크 인덱스 대상이 아니다")
				.isEqualTo(2);
	}

	@Test
	@DisplayName("존재하지 않는 지갑의 거래는 외래키가 막는다")
	void 없는_지갑의_거래는_넣을_수_없다() {
		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO point_transactions
					(wallet_id, transaction_type, transaction_direction, amount, balance_after, idempotency_key)
				VALUES (?, CAST(? AS point_transactions_transaction_type), ?, ?, ?, ?)
				""", 999_999, "DEPOSIT", "GRANT", 100, 100, "orphan"))
				.isInstanceOf(DataAccessException.class);
	}

	@Test
	@DisplayName("한 사용자에게 지갑을 두 개 만들 수 없다")
	void 지갑은_사용자당_하나다() {
		assertThatThrownBy(() -> jdbcTemplate.update(
				"INSERT INTO point_wallets (user_id, balance) VALUES (?, 0)", childId))
				.isInstanceOf(DataAccessException.class);
	}

	private void insertTransaction(int amount, int balanceAfter, String idempotencyKey) {
		jdbcTemplate.update("""
				INSERT INTO point_transactions
					(wallet_id, transaction_type, transaction_direction, amount, balance_after, idempotency_key)
				VALUES (?, CAST(? AS point_transactions_transaction_type), ?, ?, ?, ?)
				""", walletId, "DEPOSIT", "GRANT", amount, balanceAfter, idempotencyKey);
	}
}
