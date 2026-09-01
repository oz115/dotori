package com.bana.dotori.mock.store;

/**
 * 상점 물품 한 건. API 명세 SHOP-02(상품 상세 조회)와 store_products 테이블의
 * 컬럼 구성을 그대로 따른다.
 *
 * @param storeProductId 상품 ID
 * @param familyId       이 상품이 속한 가족. 상점은 가족 단위다
 * @param name           상품명
 * @param price          단가(포인트)
 * @param quantity       남은 재고
 */
public record StoreProductSnapshot(
		Integer storeProductId,
		Integer familyId,
		String name,
		int price,
		int quantity) {

	public boolean hasStock(int requested) {
		return quantity >= requested;
	}

	public StoreProductSnapshot reduceStock(int requested) {
		return new StoreProductSnapshot(storeProductId, familyId, name, price, quantity - requested);
	}

	public int totalPrice(int requested) {
		return price * requested;
	}
}
