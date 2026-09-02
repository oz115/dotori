package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointTransactionRepository;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * 4계층 - 잔액 변경과 이력 저장이 정말 하나의 트랜잭션인지 검증한다.
 *
 * <p>테스트 메서드에 {@code @Transactional} 을 붙이지 않는다.
 * 테스트가 통째로 롤백되면 "서비스가 롤백했는지"를 구분할 수 없기 때문이다.
 * 대신 커밋된 실제 DB 상태를 직접 조회해서 확인하고 뒷정리도 손으로 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("포인트 변경의 원자성")
class PointWalletServiceTransactionTest {

	@Autowired
	private PointWalletService pointWalletService;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@MockitoSpyBean
	private PointTransactionRepository pointTransactionRepository;

	private Integer childId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-atomicity");
		pointWalletRepository.save(PointWallet.openWithBalance(childId, 1_000));
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
	}

	@Test
	@DisplayName("이력 저장이 실패하면 이미 바꾼 잔액도 함께 되돌린다")
	void 이력_저장_실패시_잔액이_롤백된다() {
		willThrow(new DataAccessResourceFailureException("ledger insert failed"))
				.given(pointTransactionRepository).saveAndFlush(any(PointTransaction.class));

		assertThatThrownBy(() -> pointWalletService.deposit(
				PointChangeCommand.of(childId, 500, PointTransactionReason.GRANT, "부모 지급", "grant-rollback")))
				.isInstanceOf(DataAccessResourceFailureException.class);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId))
				.as("이력이 안 남았으면 잔액도 늘어나 있으면 안 된다")
				.isEqualTo(1_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
	}

	@Test
	@DisplayName("차감이 거부되면 잔액과 이력 모두 그대로다")
	void 차감_거부시_아무것도_바뀌지_않는다() {
		assertThatThrownBy(() -> pointWalletService.withdraw(
				PointChangeCommand.of(childId, 1_001, PointTransactionReason.PURCHASE, "구매", "purchase-fail")))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_POINT);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(1_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
	}

	@Test
	@DisplayName("성공한 거래는 잔액과 이력이 모두 커밋된다")
	void 성공하면_둘_다_커밋된다() {
		pointWalletService.deposit(
				PointChangeCommand.of(childId, 500, PointTransactionReason.CHARGE, "충전", "charge-commit"));

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(1_500);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
	}

	@Test
	@DisplayName("호출한 쪽 트랜잭션이 실패하면 포인트 변경도 함께 되돌아간다")
	void 호출자_트랜잭션에_참여한다() {
		// 구매는 상점에 이력을 남기고 포인트를 차감한다. 둘 중 뒤엣것이 실패했을 때
		// 앞서 끝난 포인트 변경이 남아 있으면, 산 것도 없는데 포인트만 빠진 상태가 된다.
		// 그래서 포인트 변경은 반드시 호출자의 트랜잭션 안에서 함께 롤백돼야 한다.
		assertThatThrownBy(() -> transactionTemplate.execute(status -> {
			pointWalletService.withdraw(PointChangeCommand.of(
					childId, 400, PointTransactionReason.PURCHASE, "구매", "purchase-outer"));
			throw new IllegalStateException("호출한 도메인에서 실패");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId))
				.as("바깥 트랜잭션이 깨졌으면 차감도 없던 일이 돼야 한다")
				.isEqualTo(1_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
	}

	@Test
	@DisplayName("호출한 쪽 트랜잭션이 성공하면 함께 커밋된다")
	void 호출자와_함께_커밋된다() {
		transactionTemplate.execute(status -> pointWalletService.withdraw(PointChangeCommand.of(
				childId, 400, PointTransactionReason.PURCHASE, "구매", "purchase-outer-ok")));

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(600);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
	}
}
