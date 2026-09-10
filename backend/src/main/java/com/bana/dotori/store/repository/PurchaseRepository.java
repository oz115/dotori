package com.bana.dotori.store.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.bana.dotori.store.entity.Purchase;

import jakarta.persistence.criteria.Predicate;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer>,
		JpaSpecificationExecutor<Purchase> {

	/**
	 * SHOP-06 "한 상품당 하루 1회 제한" 판정. purchase_date 기준이다.
	 */
	boolean existsByChildIdAndStoreProductIdAndPurchaseDate(Integer childId, Integer storeProductId,
			LocalDate purchaseDate);

	/**
	 * SHOP-07 구매 내역 조회. startDate/endDate 는 둘 다 없어도, 한쪽만 있어도 된다.
	 *
	 * <p>JPQL 에 {@code (:startDate is null or ...)} 로 쓰면 PostgreSQL 이
	 * "could not determine data type of parameter" 로 거부한다. 비교 대상 없이
	 * IS NULL 로만 쓰인 파라미터는 타입을 추론할 수 없기 때문이다. WLT-06
	 * ({@link com.bana.dotori.wallet.repository.PointTransactionRepository#search})과 같은
	 * 이유로, 조건이 있을 때만 Predicate 를 붙이는 Criteria 로 조립한다.
	 *
	 * <p>최신 구매가 위로 오도록 정렬하고, 같은 시각이면 ID 로 순서를 고정한다.
	 */
	default List<Purchase> findAllByChildIdAndPeriod(Integer childId, LocalDate startDate, LocalDate endDate) {
		Sort sort = Sort.by(Sort.Direction.DESC, "purchasedAt", "id");
		return findAll((root, query, builder) -> {
			List<Predicate> conditions = new ArrayList<>();
			conditions.add(builder.equal(root.get("childId"), childId));
			if (startDate != null) {
				conditions.add(builder.greaterThanOrEqualTo(root.get("purchaseDate"), startDate));
			}
			if (endDate != null) {
				conditions.add(builder.lessThanOrEqualTo(root.get("purchaseDate"), endDate));
			}
			return builder.and(conditions.toArray(new Predicate[0]));
		}, sort);
	}
}
