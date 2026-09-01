package com.bana.dotori.wallet.entity;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 잔액이 바뀐 사실을 남기는 원장 한 줄.
 *
 * <p>한 번 쌓이면 절대 수정하지 않는다(append-only). 취소는 반대 방향 거래를 새로 남긴다.
 * {@code balanceAfter} 를 함께 저장하므로, 거래를 순서대로 재생했을 때의 잔액과
 * 지갑의 현재 잔액이 항상 일치해야 한다. 이 불변식이 통합 테스트의 핵심 검증 대상이다.
 */
@Entity
@Getter
@Table(name = "point_transactions", uniqueConstraints = {
		// 중복 충전·지급·차감을 막는 최종 방어선. 애플리케이션 락이 뚫려도 DB 가 거른다.
		// PostgreSQL 은 NULL 을 서로 다른 값으로 보므로, 멱등키 없는 내부 거래는 여러 건 허용된다.
		@UniqueConstraint(name = "uk_point_transactions_idempotency_key", columnNames = "idempotency_key")
})
@Check(name = "ck_point_transactions_amount_positive", constraints = "amount > 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "point_transaction_id")
	private Integer pointTransactionId;

	/**
	 * 지갑은 다른 애그리거트라 연관관계 대신 식별자만 들고 있는다.
	 * 회원가입 담당자가 만드는 User 엔티티와의 결합도 함께 피한다.
	 */
	@Column(name = "wallet_id", nullable = false, updatable = false)
	private Integer walletId;

	/**
	 * ERD 상으로는 PG 네이티브 ENUM(point_transactions_transaction_type)이지만 varchar 로 둔다.
	 * ddl-auto:update 가 재기동 시 enum 타입을 다루지 못해 컬럼이 깨지기 때문이며,
	 * users.role 도 같은 이유로 varchar 다. 스키마 관리가 스크립트나 Flyway 로 넘어가면
	 * 네이티브 enum 으로 되돌리는 것이 맞다.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", length = 20, nullable = false, updatable = false)
	private PointTransactionType transactionType;

	/**
	 * 사유. 컬럼명은 transaction_direction 이지만 실제로 담는 값은 사유다.
	 * 자세한 배경은 {@link PointTransactionReason} 주석 참고.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_direction", nullable = false, length = 10, updatable = false)
	private PointTransactionReason reason;

	@Column(name = "amount", nullable = false, updatable = false)
	private int amount;

	@Column(name = "balance_after", nullable = false, updatable = false)
	private int balanceAfter;

	@Column(name = "description", length = 255, updatable = false)
	private String description;

	/**
	 * 같은 요청이 두 번 들어와도 한 번만 반영되게 하는 키.
	 * 스케줄러 지급처럼 중복 위험이 없는 거래는 null 을 허용한다.
	 */
	@Column(name = "idempotency_key", length = 100, updatable = false)
	private String idempotencyKey;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@SuppressWarnings("checkstyle:ParameterNumber")
	private PointTransaction(Integer walletId, PointTransactionType transactionType, PointTransactionReason reason,
			int amount, int balanceAfter, String description, String idempotencyKey) {
		this.walletId = walletId;
		this.transactionType = transactionType;
		this.reason = reason;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.description = description;
		this.idempotencyKey = idempotencyKey;
		this.createdAt = OffsetDateTime.now(KST);
	}

	public static PointTransaction record(Integer walletId, PointTransactionType transactionType,
			PointTransactionReason reason, int amount, int balanceAfter, String description, String idempotencyKey) {
		return new PointTransaction(walletId, transactionType, reason, amount, balanceAfter, description,
				idempotencyKey);
	}

	public boolean isDeposit() {
		return this.transactionType == PointTransactionType.DEPOSIT;
	}
}
