package com.bana.dotori.wallet.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * 3계층 - 엔티티가 실제 PostgreSQL 스키마와 맞물리는지 검증한다.
 *
 * <p>여기가 실패하면 비즈니스 로직이 아니라 매핑·제약조건 문제다.
 * 특히 {@code transaction_type} 은 네이티브 ENUM 이라 VARCHAR 로 매핑하면 여기서만 깨진다.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("포인트 원장 영속성")
class PointTransactionRepositoryTest {

	@Autowired
	private PointTransactionRepository pointTransactionRepository;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer childId;

	@BeforeEach
	void setUp() {
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-persistence");
		pointWalletRepository.save(PointWallet.openWithBalance(childId, 1_000));
	}

	@Test
	@DisplayName("네이티브 ENUM 컬럼에 거래 유형이 저장되고 그대로 읽힌다")
	void 거래유형_ENUM이_왕복한다() {
		PointTransaction saved = pointTransactionRepository.saveAndFlush(PointTransaction.record(
				childId, PointTransactionReason.PURCHASE,
				300, 700, "상점 구매", "purchase-0001"));

		PointTransaction found = pointTransactionRepository.findById(saved.getPointTransactionId()).orElseThrow();

		assertThat(found.getReason()).isEqualTo(PointTransactionReason.PURCHASE);
		assertThat(found.getAmount()).isEqualTo(300);
		assertThat(found.getBalanceAfter()).isEqualTo(700);
		assertThat(found.getCreatedAt()).as("TIMESTAMPTZ 가 채워져야 한다").isNotNull();
	}

	@Test
	@DisplayName("가장 긴 사유(SAVINGS_WITHDRAWAL, 18자)도 ENUM 컬럼에 들어간다")
	void 가장_긴_사유도_저장된다() {
		PointTransaction saved = pointTransactionRepository.saveAndFlush(PointTransaction.record(
				childId, PointTransactionReason.SAVINGS_WITHDRAWAL,
				100, 900, "예적금 만기 수령", "invest-0001"));

		assertThat(pointTransactionRepository.findById(saved.getPointTransactionId()).orElseThrow().getReason())
				.isEqualTo(PointTransactionReason.SAVINGS_WITHDRAWAL);
	}

	@Test
	@DisplayName("같은 멱등키로 두 건을 저장하려 하면 유니크 인덱스가 막는다")
	void 멱등키는_중복될_수_없다() {
		pointTransactionRepository.saveAndFlush(PointTransaction.record(
				childId, PointTransactionReason.CHARGE,
				500, 1_500, "충전", "duplicated-key"));

		assertThatThrownBy(() -> pointTransactionRepository.saveAndFlush(PointTransaction.record(
				childId, PointTransactionReason.CHARGE,
				500, 2_000, "충전", "duplicated-key")))
				.as("애플리케이션 락이 뚫려도 DB 가 마지막으로 막아야 한다")
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("멱등키가 없는 거래는 여러 건 저장할 수 있다")
	void 멱등키가_없으면_여러건_허용된다() {
		pointTransactionRepository.saveAndFlush(PointTransaction.record(
				childId, PointTransactionReason.ALLOWANCE,
				100, 1_100, "정기 지급", null));
		pointTransactionRepository.saveAndFlush(PointTransaction.record(
				childId, PointTransactionReason.ALLOWANCE,
				100, 1_200, "정기 지급", null));

		assertThat(pointTransactionRepository.findAllByChildIdOrderByPointTransactionIdAsc(childId))
				.as("NULL 멱등키는 부분 유니크 인덱스 대상이 아니다")
				.hasSize(2);
	}

	@Test
	@DisplayName("멱등키로 이 지갑의 이전 거래를 찾을 수 있다")
	void 멱등키로_조회된다() {
		pointTransactionRepository.saveAndFlush(PointTransaction.record(
				childId, PointTransactionReason.GRANT,
				700, 1_700, "부모 지급", "grant-0001"));

		Optional<PointTransaction> found =
				pointTransactionRepository.findByChildIdAndIdempotencyKey(childId, "grant-0001");

		assertThat(found).isPresent();
		assertThat(found.orElseThrow().getAmount()).isEqualTo(700);
	}

	@Test
	@DisplayName("같은 멱등키라도 다른 지갑의 거래는 걸리지 않는다")
	void 다른_지갑의_멱등키는_조회되지_않는다() {
		pointTransactionRepository.saveAndFlush(PointTransaction.record(
				childId, PointTransactionReason.GRANT,
				700, 1_700, "부모 지급", "shared-key"));

		Integer otherChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-other-ledger");
		Integer otherWalletId = pointWalletRepository.saveAndFlush(
				PointWallet.openWithBalance(otherChildId, 0)).getChildId();

		// 멱등키는 클라이언트가 정하는 값이라 사용자끼리 겹칠 수 있다.
		// 전역으로 조회하면 남의 거래가 걸려 나와 이번 요청이 조용히 삼켜진다.
		assertThat(pointTransactionRepository.findByChildIdAndIdempotencyKey(otherWalletId, "shared-key"))
				.isEmpty();
	}

	@Test
	@DisplayName("다른 지갑이면 같은 멱등키로도 거래를 남길 수 있다")
	void 다른_지갑이면_같은_멱등키를_쓸_수_있다() {
		pointTransactionRepository.saveAndFlush(PointTransaction.record(
				childId, PointTransactionReason.CHARGE,
				500, 1_500, "충전", "collision-key"));

		Integer otherChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-other-key");
		Integer otherWalletId = pointWalletRepository.saveAndFlush(
				PointWallet.openWithBalance(otherChildId, 0)).getChildId();

		assertThatCode(() -> pointTransactionRepository.saveAndFlush(PointTransaction.record(
				otherWalletId, PointTransactionReason.CHARGE,
				500, 500, "충전", "collision-key")))
				.as("유일성은 (지갑, 키) 범위여야 한다")
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("한 사용자에게 지갑이 두 개 만들어지지 않는다")
	void 지갑은_사용자당_하나다() {
		Integer childId = WalletTestFixture.insertChild(jdbcTemplate, "child-duplicate-wallet");
		pointWalletRepository.saveAndFlush(PointWallet.openFor(childId));

		assertThatThrownBy(() -> pointWalletRepository.saveAndFlush(PointWallet.openFor(childId)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
