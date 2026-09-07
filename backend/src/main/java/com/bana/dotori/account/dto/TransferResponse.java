package com.bana.dotori.account.dto;

import java.time.OffsetDateTime;

public record TransferResponse(
	String fromAccountNo,
	String toAccountNo,
	Long amount,
	OffsetDateTime transactedAt
) {
}
