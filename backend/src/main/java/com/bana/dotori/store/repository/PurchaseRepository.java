package com.bana.dotori.store.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.store.entity.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {

	/**
	 * SHOP-06 "한 상품당 하루 1회 제한" 판정. purchase_date 기준이다.
	 */
	boolean existsByChildIdAndStoreProductIdAndPurchaseDate(Integer childId, Integer storeProductId,
			LocalDate purchaseDate);
}
