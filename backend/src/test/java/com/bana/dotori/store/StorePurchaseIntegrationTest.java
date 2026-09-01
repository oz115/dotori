package com.bana.dotori.store;

import com.bana.dotori.global.error.ErrorCode;
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

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.exception.InsufficientPointException;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * 통합 - 포인트 차감(WLT-05)이 실제 호출자(상점 구매) 흐름에서 동작하는지 확인한다.
 *
 * <p>상점은 아직 목업이지만 포인트 쪽은 진짜 DB 다.
 * 차감이 원장에 PURCHASE 로 남고 잔액이 실제로 줄어드는지가 검증 대상이다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("상점 구매를 통한 포인트 차감")
class StorePurchaseIntegrationTest {

	private static final Integer FAMILY_ID = 1;

	@Autowired
	private StorePurchaseService storePurchaseService;

	@Autowired
	private MockStoreRepository mockStoreRepository;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer childId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		mockStoreRepository.clear();
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-purchase");
		pointWalletRepository.save(PointWallet.openWithBalance(childId, 20_000));
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		mockStoreRepository.clear();
	}

	@Test
	@DisplayName("구매하면 포인트가 차감되고 원장에 PURCHASE 가 남는다")
	void 구매하면_차감된다() {
		StoreProductSnapshot product =
				mockStoreRepository.registerProduct(FAMILY_ID, "게임 30분 이용권", 3_000, 5);

		StorePurchaseResult result = storePurchaseService.purchase(
				FAMILY_ID, childId, product.storeProductId(), 1);

		assertThat(result.totalPrice()).isEqualTo(3_000);
		assertThat(result.pointBalanceAfter()).isEqualTo(17_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(17_000);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT transaction_direction FROM point_transactions", String.class))
				.isEqualTo("PURCHASE");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT transaction_type FROM point_transactions", String.class))
				.isEqualTo("WITHDRAWAL");
	}

	@Test
	@DisplayName("수량이 여러 개면 단가 곱만큼 차감된다")
	void 수량만큼_차감된다() {
		StoreProductSnapshot product =
				mockStoreRepository.registerProduct(FAMILY_ID, "간식 쿠폰", 2_000, 10);

		StorePurchaseResult result = storePurchaseService.purchase(
				FAMILY_ID, childId, product.storeProductId(), 3);

		assertThat(result.totalPrice()).isEqualTo(6_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(14_000);
	}

	@Test
	@DisplayName("포인트가 부족하면 구매가 실패하고 잔액이 그대로다")
	void 포인트가_부족하면_실패한다() {
		StoreProductSnapshot product =
				mockStoreRepository.registerProduct(FAMILY_ID, "비싼 장난감", 50_000, 1);

		assertThatThrownBy(() -> storePurchaseService.purchase(
				FAMILY_ID, childId, product.storeProductId(), 1))
				.isInstanceOf(InsufficientPointException.class);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(20_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate))
				.as("실패한 구매는 원장에 남지 않아야 한다")
				.isZero();
	}

	@Test
	@DisplayName("재고가 없으면 포인트를 건드리지 않고 막는다")
	void 재고가_없으면_실패한다() {
		StoreProductSnapshot product =
				mockStoreRepository.registerProduct(FAMILY_ID, "품절 상품", 1_000, 0);

		assertThatThrownBy(() -> storePurchaseService.purchase(
				FAMILY_ID, childId, product.storeProductId(), 1))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(20_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
	}

	@Test
	@DisplayName("같은 상품을 하루에 두 번 사면 두 번째는 막힌다")
	void 하루_한번_제한이_걸린다() {
		StoreProductSnapshot product =
				mockStoreRepository.registerProduct(FAMILY_ID, "게임 30분 이용권", 3_000, 5);

		storePurchaseService.purchase(FAMILY_ID, childId, product.storeProductId(), 1);

		assertThatThrownBy(() -> storePurchaseService.purchase(
				FAMILY_ID, childId, product.storeProductId(), 1))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DAILY_LIMIT_EXCEEDED);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId))
				.as("두 번째 구매가 막혔으면 한 번치만 빠져 있어야 한다")
				.isEqualTo(17_000);
	}

	@Test
	@DisplayName("다른 가족의 상품은 살 수 없다")
	void 다른_가족_상품은_살_수_없다() {
		StoreProductSnapshot otherFamilyProduct =
				mockStoreRepository.registerProduct(999, "남의 집 상품", 1_000, 5);

		assertThatThrownBy(() -> storePurchaseService.purchase(
				FAMILY_ID, childId, otherFamilyProduct.storeProductId(), 1))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
	}
}
