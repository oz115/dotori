package com.bana.dotori.store.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상점 구매 이력 (SHOP-06).
 *
 * <p>quantity·price 는 구매 시점 값을 그대로 남긴다 — 이후 상품 가격이 바뀌어도
 * 이미 산 구매 이력은 그때 가격으로 고정돼야 한다.
 */
@Entity
@Table(name = "purchases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Purchase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "purchase_id")
	private Integer id;

	@Column(name = "family_id", nullable = false)
	private Integer familyId;

	@Column(name = "child_id", nullable = false)
	private Integer childId;

	@Column(name = "store_product_id", nullable = false)
	private Integer storeProductId;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@Column(name = "price", nullable = false)
	private int price;

	@Column(name = "purchase_date", nullable = false)
	private LocalDate purchaseDate;

	@CreationTimestamp
	@Column(name = "purchased_at", nullable = false, updatable = false)
	private OffsetDateTime purchasedAt;

	@Builder
	private Purchase(Integer familyId, Integer childId, Integer storeProductId, int quantity, int price,
			LocalDate purchaseDate) {
		this.familyId = familyId;
		this.childId = childId;
		this.storeProductId = storeProductId;
		this.quantity = quantity;
		this.price = price;
		this.purchaseDate = purchaseDate;
	}
}
