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

	/** AI 가 value·price 와 함께 산정하는 근거 문구. 산정 전에는 null. */
	@Column(name = "reason")
	private String reason;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Builder
	private StoreProduct(Integer familyId, Integer createdBy, String name, String description,
			StoreProductCategory category, BigDecimal value, Integer price, String reason, int quantity) {
		this.familyId = familyId;
		this.createdBy = createdBy;
		this.name = name;
		this.description = description;
		this.category = category;
		this.value = value;
		this.price = price;
		this.reason = reason;
		this.quantity = quantity;
	}

	/**
	 * 상품 정보 부분 수정 (SHOP-04). null 인 필드는 그대로 둔다.
	 *
	 * <p>name/description/category 가 바뀌어도 value 는 건드리지 않는다 — AI 가 이미 산정한 값을
	 * 무효화하는 재평가 로직은 아직 없다(추후 별도 작업). price 는 AI 산정값이지만 부모가 직접
	 * 덮어쓸 수 있게 열어 둔다.
	 */
	public void update(String name, String description, StoreProductCategory category, Integer price,
			Integer quantity) {
		if (name != null) {
			this.name = name;
		}
		if (description != null) {
			this.description = description;
		}
		if (category != null) {
			this.category = category;
		}
		if (price != null) {
			this.price = price;
		}
		if (quantity != null) {
			this.quantity = quantity;
		}
	}

	/**
	 * 상품 삭제 (SHOP-05). 구매 이력(purchases)이 이 상품을 참조하므로 hard delete 대신
	 * soft delete 한다 — 이미 팔린 상품을 지워버리면 구매 이력에서 상품 정보를 잃는다.
	 */
	public void softDelete() {
		this.deletedAt = OffsetDateTime.now();
	}

	/**
	 * 구매 확정 시 재고를 줄인다 (SHOP-06). 호출자(StorePurchaseService)가 구매 전에
	 * 재고 충분 여부를 이미 확인했으므로 여기서는 다시 검증하지 않는다.
	 */
	public void reduceStock(int amount) {
		this.quantity -= amount;
	}
}
