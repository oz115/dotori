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
@Table(name = "linked_accounts", uniqueConstraints = {
	@UniqueConstraint(name = "uk_linked_accounts_user_account", columnNames = {"user_id", "account_number"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "account_id")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "bank_code", length = 50, nullable = false)
	private String bankCode;

	@Column(name = "bank_name", length = 100, nullable = false)
	private String bankName;

	@Column(name = "account_number", length = 100, nullable = false)
	private String accountNumber;

	/** 금융망이 계좌 예금주명을 안 내려주는 경우가 있어 nullable 로 둔다. */
	@Column(name = "account_name", length = 100)
	private String accountName;

	@Column(name = "finance_user_key", length = 100, nullable = false)
	private String financeUserKey;

	@CreationTimestamp
	@Column(name = "linked_at", nullable = false, updatable = false)
	private OffsetDateTime linkedAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Builder
	private Account(User user, String bankCode, String bankName, String accountNumber,
		String accountName, String financeUserKey) {
		this.user = user;
		this.bankCode = bankCode;
		this.bankName = bankName;
		this.accountNumber = accountNumber;
		this.accountName = accountName;
		this.financeUserKey = financeUserKey;
	}

}
