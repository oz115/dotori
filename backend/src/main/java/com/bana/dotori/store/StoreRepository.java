package com.bana.dotori.store;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 상점 도메인(SHOP-01~07)이 완성될 때까지 쓰는 경계.
 *
 * <p>상점은 다른 담당자의 작업 범위다. 포인트 차감(WLT-05)이 실제로 동작하는지 검증하려면
 * 호출자가 하나는 있어야 해서, 상점이 제공할 기능만 인터페이스로 뽑아 두고
 * 메모리 구현을 붙였다. 실제 상점이 들어오면 이 인터페이스를 구현하고
 * {@link MockStoreRepository} 를 지우면 된다.
 */
public interface StoreRepository {

	Optional<StoreProductSnapshot> findProduct(Integer familyId, Integer storeProductId);

	/**
	 * 재고를 줄인다. 구매가 확정될 때만 호출한다.
	 */
	void reduceStock(Integer storeProductId, int quantity);

	/**
	 * SHOP-06 의 "한 상품당 하루 1회 제한"을 판정한다.
	 * purchases 테이블의 {@code purchase_date} 기준이다.
	 */
	boolean hasPurchasedOn(Integer childId, Integer storeProductId, LocalDate date);

	/**
	 * 구매 이력을 남기고 구매 ID 를 돌려준다.
	 *
	 * <p>날짜를 호출자에게서 받는 이유는 하루 1회 제한 판정과 기록이 같은 기준을 써야 하기 때문이다.
	 * 저장소가 제 나름의 시각을 쓰면 서버 타임존에 따라 두 날짜가 어긋나 제한이 뚫린다.
	 */
	Integer savePurchase(Integer familyId, Integer childId, Integer storeProductId, int quantity, int unitPrice,
			LocalDate purchaseDate);
}
