package com.bana.dotori.store.entity;

/**
 * 상점 상품 카테고리.
 *
 * <p>상품 이미지는 카테고리별로 S3에 준비되므로 상품에는 카테고리만 저장한다.
 * DB의 store_products_category enum 타입과 값이 같아야 한다.
 */
public enum StoreProductCategory {
	GAME,
	SNACK,
	TOY,
	OUTING,
	PRIVILEGE,
	ETC
}
