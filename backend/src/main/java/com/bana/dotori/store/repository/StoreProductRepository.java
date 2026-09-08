package com.bana.dotori.store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.store.entity.StoreProduct;

public interface StoreProductRepository extends JpaRepository<StoreProduct, Integer> {

	/**
	 * 가족 상점의 상품 목록 (SHOP-01). 부모가 방금 등록한 상품이 위에 오도록 최신순으로 준다.
	 * created_at 이 같은 행은 ID 로 순서를 고정한다. 삭제된 상품은 제외한다.
	 */
	List<StoreProduct> findAllByFamilyIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Integer familyId);

	/**
	 * 상품 상세/수정/삭제 (SHOP-02·04·05). family_id 를 조건에 같이 넣어, 존재는 하지만 다른 가족
	 * 소유인 상품과 아예 없는 상품을 구분하지 않고 똑같이 빈 결과로 준다. 남의 상품이 있다는 사실
	 * 자체를 노출하지 않기 위해서다. 이미 삭제된 상품도 존재하지 않는 것과 동일하게 취급한다.
	 */
	Optional<StoreProduct> findByIdAndFamilyIdAndDeletedAtIsNull(Integer storeProductId, Integer familyId);
}
