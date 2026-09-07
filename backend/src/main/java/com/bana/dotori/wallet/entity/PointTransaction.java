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
import jakarta.persistence.Index;
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
		//
		// 지갑까지 묶어야 한다. 멱등키만으로 유일하게 두면 서로 다른 두 사용자가 우연히 같은
		// Idempotency-Key 를 보냈을 때 뒤에 온 요청이 남의 거래를 자기 것으로 착각한다.
		// 키는 클라이언트가 정하는 값이라 충돌을 막을 방법이 없으므로, 유일성 범위를 지갑 안으로 좁힌다.
		@UniqueConstraint(name = "UQ_POINT_TRANSACTIONS_CHILD_IDEMPOTENCY_KEY",
				columnNames = {"child_id", "idempotency_key"})
}, indexes = {
		// WLT-06 거래내역 조회가 쓰는 인덱스. 정렬 키와 순서가 같아야 한다.
		// 커서 페이징은 "이 지점 이후"를 인덱스에서 바로 찾아 limit 만큼 읽고 멈추는 것이
		// 핵심이라, 이 인덱스가 없으면 지갑의 거래를 전부 읽고 정렬한 뒤 잘라낸다.
		//
		// 방향은 ASC 로만 만든다. PostgreSQL 은 인덱스를 역방향으로도 스캔하므로
		// 최신순(DESC) 조회도 같은 인덱스를 쓴다.
		@Index(name = "IX_POINT_TRANSACTIONS_CHILD_CREATED_AT",
				columnList = "child_id, created_at, point_transaction_id")
})
@Check(name = "CK_POINT_TRANSACTIONS_AMOUNT_POSITIVE", constraints = "amount > 0")
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
	@Column(name = "child_id", nullable = false, updatable = false)
	private Integer childId;

	/**
	 * 거래 사유. API 명세의 {@code transactionType} 필드가 이 값을 그대로 내보낸다.
	 *
	 * <p>원래 ERD 에서는 이 컬럼이 DEPOSIT/WITHDRAWAL 을 담는 네이티브 ENUM 이고
	 * 사유는 transaction_direction 에 넣고 있었다. 팀에서 direction 컬럼을 쓰지 않기로 해
	 * 컬럼을 없애고 사유를 이쪽으로 옮겼다. 명세가 기대하던 모양과도 이제 일치한다.
	 *
	 * <p>잔액이 늘었는지 줄었는지는 따로 저장하지 않는다. {@code balance_after} 의 변화로
	 * 알 수 있고, 같은 정보를 두 곳에 두면 어긋날 여지만 생긴다.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", length = 20, nullable = false, updatable = false)
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
	 *
	 * <p>유일성은 지갑 안에서만 보장된다. 클라이언트가 정하는 값이라 사용자끼리 겹칠 수 있고,
	 * 전역으로 유일하게 두면 남의 키와 충돌한 요청이 조용히 삼켜진다.
	 */
	@Column(name = "idempotency_key", length = 100, updatable = false)
	private String idempotencyKey;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	private PointTransaction(Integer childId, PointTransactionReason reason,
			int amount, int balanceAfter, String description, String idempotencyKey) {
		this.childId = childId;
		this.reason = reason;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.description = description;
		this.idempotencyKey = idempotencyKey;
		this.createdAt = OffsetDateTime.now(KST);
	}

	public static PointTransaction record(Integer childId, PointTransactionReason reason,
			int amount, int balanceAfter, String description, String idempotencyKey) {
		return new PointTransaction(childId, reason, amount, balanceAfter, description, idempotencyKey);
	}
}
