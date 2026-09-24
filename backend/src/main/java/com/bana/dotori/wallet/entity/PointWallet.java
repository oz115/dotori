package com.bana.dotori.wallet.entity;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 1명이 보유한 포인트 잔액.
 *
 * <p>잔액을 바꾸는 규칙(금액 검증·잔액 부족·상한 초과)을 전부 이 클래스가 갖는다.
 * 스프링 컨텍스트 없이 순수 단위 테스트로 검증할 수 있게 하려는 의도이며,
 * 잔액 계산이 깨지면 {@code PointWalletTest} 가 가장 먼저 실패해 원인 위치를 좁혀준다.
 */
@Entity
@Getter
@Table(name = "point_wallets", uniqueConstraints = {
		// 한 사용자에게 지갑이 두 개 생기면 잔액 조회가 어느 쪽을 골라야 할지 알 수 없다.
		@UniqueConstraint(name = "UQ_POINT_WALLETS_CHILD_ID", columnNames = "child_id")
})
@Check(name = "CK_POINT_WALLETS_BALANCE_NON_NEGATIVE", constraints = "balance >= 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointWallet {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "wallet_id")
	private Integer walletId;

	@Column(name = "child_id", nullable = false, unique = true, updatable = false)
	private Integer childId;

	@Column(name = "balance", nullable = false)
	private int balance;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	private PointWallet(Integer childId, int balance) {
		OffsetDateTime now = OffsetDateTime.now(KST);
		this.childId = childId;
		this.balance = balance;
		this.createdAt = now;
		this.updatedAt = now;
	}

	/**
	 * 잔액 0인 새 지갑을 만든다. 회원가입 또는 가족 합류 시점에 호출한다.
	 */
	public static PointWallet openFor(Integer childId) {
		return new PointWallet(childId, 0);
	}

	/**
	 * 테스트에서 초기 잔액이 있는 지갑이 필요할 때 쓴다.
	 */
	public static PointWallet openWithBalance(Integer childId, int initialBalance) {
		return new PointWallet(childId, initialBalance);
	}

	/**
	 * 포인트를 더하고 변경된 잔액을 돌려준다.
	 *
	 * @throws com.bana.dotori.global.error.BusinessException INVALID_POINT_AMOUNT 금액이 0 이하인 경우
	 *         또는 POINT_BALANCE_EXCEEDED 잔액이 INTEGER 상한을 넘는 경우
	 */
	public int deposit(int amount) {
		validateAmount(amount);
		long next = (long)this.balance + amount;
		if (next > Integer.MAX_VALUE) {
			throw new BusinessException(ErrorCode.POINT_BALANCE_EXCEEDED,
					"보유 가능한 최대 포인트를 초과했습니다. 잔액=" + this.balance + ", 지급 요청=" + amount
							+ ", 상한=" + Integer.MAX_VALUE);
		}
		this.balance = (int)next;
		return this.balance;
	}

	/**
	 * 포인트를 빼고 변경된 잔액을 돌려준다.
	 *
	 * @throws com.bana.dotori.global.error.BusinessException INVALID_POINT_AMOUNT 금액이 0 이하인 경우
	 *         또는 INSUFFICIENT_POINT 잔액이 부족한 경우
	 */
	public int withdraw(int amount) {
		validateAmount(amount);
		if (this.balance < amount) {
			throw new BusinessException(ErrorCode.INSUFFICIENT_POINT,
					"보유 포인트가 부족합니다. 잔액=" + this.balance + ", 차감 요청=" + amount);
		}
		this.balance -= amount;
		return this.balance;
	}

	private void validateAmount(int amount) {
		if (amount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_POINT_AMOUNT,
					"포인트 금액은 1 이상이어야 합니다. 요청 금액=" + amount);
		}
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = OffsetDateTime.now(KST);
	}
}
