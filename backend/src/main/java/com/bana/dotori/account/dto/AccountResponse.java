package com.bana.dotori.account.dto;

import java.time.OffsetDateTime;

public record AccountResponse(
	Integer id,
	String bankCode,
	String bankName,
	String accountNo,
	OffsetDateTime createdAt
) {
}
