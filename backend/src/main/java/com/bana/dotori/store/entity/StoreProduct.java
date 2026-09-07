package com.bana.dotori.store.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가족 상점 상품 (SHOP).
 *
 * <p>value 는 AI 서버가 비동기로 산정하는 가치 가중치(아이 하루 소득 D의 배수)다.
 * price = value * D 이므로, 산정 전에는 value 와 price 모두 null 이다
 * (null = 가격 산정 중).
 */
@Entity
@Table(name = "store_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "store_product_id")
	private Integer id;

	@Column(name = "family_id", nullable = false)
	private Integer familyId;

	@Column(name = "created_by", nullable = false)
	private Integer createdBy;

	@Column(name = "name", length = 100, nullable = false)
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false)
	private StoreProductCategory category;

	@Column(name = "value", precision = 10, scale = 2)
	private BigDecimal value;

	@Column(name = "price")
	private Integer price;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Builder
	private StoreProduct(Integer familyId, Integer createdBy, String name, String description,
			StoreProductCategory category, BigDecimal value, Integer price, int quantity) {
		this.familyId = familyId;
		this.createdBy = createdBy;
		this.name = name;
		this.description = description;
		this.category = category;
		this.value = value;
		this.price = price;
		this.quantity = quantity;
	}
}
