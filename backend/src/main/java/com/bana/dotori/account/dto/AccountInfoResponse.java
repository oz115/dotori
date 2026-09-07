package com.bana.dotori.account.dto;

public record AccountInfoResponse(
	String bankCode,
	String bankName,
	String accountNo,
	String accountName,
	String accountTypeName,
	Long accountBalance,
	Long dailyTransferLimit,
	Long oneTimeTransferLimit,
	String currency
) {
}
