package com.bana.dotori.account.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.bana.dotori.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts", uniqueConstraints = {
	@UniqueConstraint(name = "uk_accounts_user_account", columnNames = {"user_id", "account_no"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "account_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "bank_code", length = 20, nullable = false)
	private String bankCode;

	@Column(name = "bank_name", length = 50, nullable = false)
	private String bankName;

	@Column(name = "account_no", length = 50, nullable = false)
	private String accountNo;

	@Column(name = "finance_user_key", length = 100, nullable = false)
	private String financeUserKey;

	@Column(name = "is_primary", nullable = false)
	private boolean primary;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Builder
	private Account(User user, String bankCode, String bankName, String accountNo, String financeUserKey) {
		this.user = user;
		this.bankCode = bankCode;
		this.bankName = bankName;
		this.accountNo = accountNo;
		this.financeUserKey = financeUserKey;
		this.primary = true;
	}

	public void markDeleted() {
		this.deletedAt = OffsetDateTime.now();
	}
}
