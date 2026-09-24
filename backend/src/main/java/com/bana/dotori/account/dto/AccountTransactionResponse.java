package com.bana.dotori.account.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record AccountTransactionResponse(
	String transactionUniqueNo,
	TransactionDirection transactionType,
	String transactionTypeName,
	Integer amount,
	Integer balanceAfter,
	String description,
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
	OffsetDateTime transactedAt
) {
}
