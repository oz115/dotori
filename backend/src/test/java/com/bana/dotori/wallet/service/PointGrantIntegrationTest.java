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

import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.exception.NotFamilyChildException;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * 통합 - 지급이 실제 가족 데이터를 보고 판단하는지 확인한다.
 *
 * <p>가족 소속 검증은 family_members 를 직접 조회하므로, 가짜 저장소로는
 * 쿼리가 맞는지 알 수 없다. 여기서만 검증 가능한 부분이다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("포인트 지급 통합")
class PointGrantIntegrationTest {

	@Autowired
	private PointGrantService pointGrantService;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer familyId;
	private Integer childId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		Integer parentId = WalletTestFixture.insertParent(jdbcTemplate, "parent-grant");
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-grant");
		familyId = WalletTestFixture.insertFamilyWithChild(jdbcTemplate, parentId, childId);
		pointWalletRepository.save(PointWallet.openWithBalance(childId, 15_000));
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
	}

	@Test
	@DisplayName("가족 자녀에게 지급하면 잔액이 늘고 원장에 GRANT 가 남는다")
	void 지급_성공() {
		PointChangeResult result = pointGrantService.grant(familyId, childId, 5_000, "용돈", "grant-success");

		assertThat(result.balanceAfter()).isEqualTo(20_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(20_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT transaction_direction FROM point_transactions", String.class))
				.isEqualTo("GRANT");
	}

	@Test
	@DisplayName("다른 가족의 아이에게는 지급되지 않는다")
	void 다른_가족_아이는_거부된다() {
		Integer otherParentId = WalletTestFixture.insertParent(jdbcTemplate, "parent-other");
		Integer otherChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-other");
		WalletTestFixture.insertFamilyWithChild(jdbcTemplate, otherParentId, otherChildId);
		pointWalletRepository.save(PointWallet.openWithBalance(otherChildId, 0));

		assertThatThrownBy(() -> pointGrantService.grant(familyId, otherChildId, 5_000, "용돈", "grant-other"))
				.isInstanceOf(NotFamilyChildException.class);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, otherChildId))
				.as("남의 집 아이 지갑에 포인트가 들어가면 안 된다")
				.isZero();
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
	}

	@Test
	@DisplayName("가족에 속하지 않은 사용자에게는 지급되지 않는다")
	void 무소속_사용자는_거부된다() {
		Integer strangerId = WalletTestFixture.insertChild(jdbcTemplate, "child-stranger");

		assertThatThrownBy(() -> pointGrantService.grant(familyId, strangerId, 5_000, "용돈", "grant-stranger"))
				.isInstanceOf(NotFamilyChildException.class);
	}

	@Test
	@DisplayName("부모 본인에게는 지급할 수 없다")
	void 부모에게는_지급할_수_없다() {
		Integer parentId = jdbcTemplate.queryForObject(
				"SELECT user_id FROM users WHERE login_id = 'parent-grant'", Integer.class);

		assertThatThrownBy(() -> pointGrantService.grant(familyId, parentId, 5_000, "용돈", "grant-parent"))
				.as("가족 구성원이지만 자녀가 아니므로 거부돼야 한다")
				.isInstanceOf(NotFamilyChildException.class);
	}

	@Test
	@DisplayName("같은 멱등키로 다시 지급하면 한 번만 반영된다")
	void 중복_지급은_한번만_반영된다() {
		pointGrantService.grant(familyId, childId, 5_000, "용돈", "grant-once");
		PointChangeResult second = pointGrantService.grant(familyId, childId, 5_000, "용돈", "grant-once");

		assertThat(second.replayed()).isTrue();
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(20_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
	}
}
