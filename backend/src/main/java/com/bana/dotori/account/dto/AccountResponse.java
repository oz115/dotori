package com.bana.dotori.account.dto;

import java.time.OffsetDateTime;

public record AccountResponse(
	Long id,
	String bankCode,
	String bankName,
	String accountNo,
	boolean primary,
	OffsetDateTime createdAt
) {
}
