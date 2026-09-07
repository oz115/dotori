package com.bana.dotori.store.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.store.entity.StoreProduct;

public interface StoreProductRepository extends JpaRepository<StoreProduct, Integer> {

	/**
	 * 가족 상점의 상품 목록 (SHOP-01). 부모가 방금 등록한 상품이 위에 오도록 최신순으로 준다.
	 * created_at 이 같은 행은 ID 로 순서를 고정한다.
	 */
	List<StoreProduct> findAllByFamilyIdOrderByCreatedAtDescIdDesc(Integer familyId);
}
