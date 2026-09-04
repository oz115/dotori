package com.bana.dotori.account.dto;

import java.util.List;

public record AccountTransactionHistoryResponse(
	List<AccountTransactionResponse> transactions,
	String nextCursor,
	boolean hasNext
) {
}
