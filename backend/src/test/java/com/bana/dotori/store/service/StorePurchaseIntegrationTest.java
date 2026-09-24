package com.bana.dotori.store.service;

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
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.store.dto.StorePurchaseResponse;
import com.bana.dotori.store.entity.StoreProduct;
import com.bana.dotori.store.entity.StoreProductCategory;
import com.bana.dotori.store.repository.StoreProductRepository;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * 통합 - 포인트 차감(WLT-05)이 실제 호출자(상점 구매, SHOP-06)를 거쳐 동작하는지 확인한다.
 *
 * <p>상점이 목업이던 시절 만든 테스트를 실제 store_products/purchases 테이블 기준으로 다시 썼다.
 * 상품·가족·회원 모두 이 DB에 실제로 존재해야 하므로(FK 제약), 매번 새로 만들고
 * {@link WalletTestFixture#cleanUp} 으로 뒷정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("상점 구매를 통한 포인트 차감")
class StorePurchaseIntegrationTest {

	@Autowired
	private StorePurchaseService storePurchaseService;

	@Autowired
	private StoreProductRepository storeProductRepository;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer parentId;
	private Integer childId;
	private Integer familyId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		parentId = WalletTestFixture.insertParent(jdbcTemplate, "parent-purchase");
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-purchase");
		familyId = WalletTestFixture.insertFamilyWithChild(jdbcTemplate, parentId, childId);
		pointWalletRepository.save(PointWallet.openWithBalance(childId, 20_000));
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
	}

	private Integer createProduct(Integer familyId, Integer createdBy, String name, int price, int quantity) {
		StoreProduct product = storeProductRepository.save(StoreProduct.builder()
			.familyId(familyId)
			.createdBy(createdBy)
			.name(name)
			.category(StoreProductCategory.ETC)
			.price(price)
			.quantity(quantity)
			.build());
		return product.getId();
	}

	@Test
	@DisplayName("구매하면 포인트가 차감되고 원장에 PURCHASE 가 남는다")
	void 구매하면_차감된다() {
		Integer productId = createProduct(familyId, parentId, "게임 30분 이용권", 3_000, 5);

		StorePurchaseResponse result = storePurchaseService.purchase(familyId, childId, productId, 1);

		assertThat(result.totalPrice()).isEqualTo(3_000);
		assertThat(result.pointBalanceAfter()).isEqualTo(17_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(17_000);
		assertThat(WalletTestFixture.selectOnlyTransactionColumn(jdbcTemplate, childId, "transaction_type"))
				.isEqualTo("PURCHASE");
	}

	@Test
	@DisplayName("수량이 여러 개면 단가 곱만큼 차감된다")
	void 수량만큼_차감된다() {
		Integer productId = createProduct(familyId, parentId, "간식 쿠폰", 2_000, 10);

		StorePurchaseResponse result = storePurchaseService.purchase(familyId, childId, productId, 3);

		assertThat(result.totalPrice()).isEqualTo(6_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(14_000);
	}

	@Test
	@DisplayName("구매하면 재고가 줄어든다")
	void 구매하면_재고가_줄어든다() {
		Integer productId = createProduct(familyId, parentId, "간식 쿠폰", 2_000, 10);

		storePurchaseService.purchase(familyId, childId, productId, 3);

		StoreProduct product = storeProductRepository.findById(productId).orElseThrow();
		assertThat(product.getQuantity()).isEqualTo(7);
	}

	@Test
	@DisplayName("포인트가 부족하면 구매가 실패하고 잔액이 그대로다")
	void 포인트가_부족하면_실패한다() {
		Integer productId = createProduct(familyId, parentId, "비싼 장난감", 50_000, 1);

		assertThatThrownBy(() -> storePurchaseService.purchase(familyId, childId, productId, 1))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_POINT);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(20_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate))
				.as("실패한 구매는 원장에 남지 않아야 한다")
				.isZero();
	}

	@Test
	@DisplayName("재고가 없으면 포인트를 건드리지 않고 막는다")
	void 재고가_없으면_실패한다() {
		Integer productId = createProduct(familyId, parentId, "품절 상품", 1_000, 0);

		assertThatThrownBy(() -> storePurchaseService.purchase(familyId, childId, productId, 1))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(20_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
	}

	@Test
	@DisplayName("같은 상품을 하루에 두 번 사면 두 번째는 막힌다")
	void 하루_한번_제한이_걸린다() {
		Integer productId = createProduct(familyId, parentId, "게임 30분 이용권", 3_000, 5);

		storePurchaseService.purchase(familyId, childId, productId, 1);

		assertThatThrownBy(() -> storePurchaseService.purchase(familyId, childId, productId, 1))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DAILY_LIMIT_EXCEEDED);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId))
				.as("두 번째 구매가 막혔으면 한 번치만 빠져 있어야 한다")
				.isEqualTo(17_000);
	}

	@Test
	@DisplayName("다른 가족의 상품은 살 수 없다")
	void 다른_가족_상품은_살_수_없다() {
		Integer otherParentId = WalletTestFixture.insertParent(jdbcTemplate, "parent-other-family");
		Integer otherChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-other-family");
		Integer otherFamilyId = WalletTestFixture.insertFamilyWithChild(jdbcTemplate, otherParentId, otherChildId);
		Integer otherFamilyProductId = createProduct(otherFamilyId, otherParentId, "남의 집 상품", 1_000, 5);

		assertThatThrownBy(() -> storePurchaseService.purchase(familyId, childId, otherFamilyProductId, 1))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("가격이 아직 산정되지 않은 상품은 살 수 없다")
	void 가격_미산정_상품은_살_수_없다() {
		StoreProduct product = storeProductRepository.save(StoreProduct.builder()
			.familyId(familyId)
			.createdBy(parentId)
			.name("가격 산정 중")
			.category(StoreProductCategory.ETC)
			.quantity(5)
			.build());

		assertThatThrownBy(() -> storePurchaseService.purchase(familyId, childId, product.getId(), 1))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRICE_NOT_EVALUATED);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(20_000);
	}

	@Test
	@DisplayName("구매_재시도_포인트_부족으로_실패해도_충전_후_다시_살_수_있다")
	void 구매_재시도_포인트_부족으로_실패해도_충전_후_다시_살_수_있다() {
		Integer productId = createProduct(familyId, parentId, "게임 30분 이용권", 30_000, 5);

		assertThatThrownBy(() -> storePurchaseService.purchase(familyId, childId, productId, 1))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_POINT);

		// 실패한 구매가 하루 1회 제한을 소진해 버리면, 부모가 포인트를 채워 줘도
		// 그날은 그 상품을 살 수 없게 된다.
		jdbcTemplate.update("UPDATE point_wallets SET balance = 50000 WHERE child_id = ?", childId);

		StorePurchaseResponse retried = storePurchaseService.purchase(familyId, childId, productId, 1);

		assertThat(retried.totalPrice()).isEqualTo(30_000);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(20_000);
	}
}
