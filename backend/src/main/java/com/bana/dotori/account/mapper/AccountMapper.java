package com.bana.dotori.account.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountInfoResponse;
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.dto.AccountTransactionHistoryResponse;
import com.bana.dotori.account.dto.AccountTransactionResponse;
import com.bana.dotori.account.dto.TransactionDirection;
import com.bana.dotori.account.dto.TransferResponse;
import com.bana.dotori.account.entity.Account;
import com.bana.dotori.finance.dto.account.InquireDemandDepositAccountRec;
import com.bana.dotori.finance.dto.transaction.TransactionHistoryItem;
import com.bana.dotori.user.entity.User;

@Component
public class AccountMapper {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter TRANSACTED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final String DEPOSIT_CODE = "1";

	public AccountAuthResponse toAccountAuthResponse(String accountNo, String authText, long expiresInSeconds) {
		return new AccountAuthResponse(accountNo, authText, expiresInSeconds);
	}

	public Account toAccount(User user, AccountAuthVerifyRequest request, String financeUserKey) {
		return Account.builder()
			.user(user)
			.externalAccountId(request.accountNo())
			.bankCode(request.bankCode())
			.bankName(request.bankName())
			.accountNumber(request.accountNo())
			.balance(0)
			.financeUserKey(financeUserKey)
			.build();
	}

	public AccountResponse toAccountResponse(Account account) {
		return new AccountResponse(account.getId(), account.getBankCode(), account.getBankName(),
			account.getAccountNumber(), account.getLinkedAt());
	}

	public AccountInfoResponse toAccountInfoResponse(InquireDemandDepositAccountRec rec) {
		return new AccountInfoResponse(rec.bankCode(), rec.bankName(), rec.accountNo(), rec.accountName(),
			rec.accountTypeName(), Long.parseLong(rec.accountBalance()), Long.parseLong(rec.dailyTransferLimit()),
			Long.parseLong(rec.oneTimeTransferLimit()), rec.currency());
	}

	public AccountTransactionHistoryResponse toAccountTransactionHistoryResponse(List<TransactionHistoryItem> items,
		String nextCursor, boolean hasNext) {
		List<AccountTransactionResponse> transactions = items.stream()
			.map(this::toAccountTransactionResponse)
			.toList();
		return new AccountTransactionHistoryResponse(transactions, nextCursor, hasNext);
	}

	private AccountTransactionResponse toAccountTransactionResponse(TransactionHistoryItem item) {
		return new AccountTransactionResponse(
			item.transactionUniqueNo(),
			toTransactionDirection(item.transactionType()),
			item.transactionTypeName(),
			Integer.parseInt(item.transactionBalance()),
			Integer.parseInt(item.transactionAfterBalance()),
			item.transactionSummary(),
			toTransactedAt(item.transactionDate(), item.transactionTime()));
	}

	public TransferResponse toTransferResponse(Account fromAccount, String toAccountNo, Long amount,
		OffsetDateTime transactedAt) {
		return new TransferResponse(fromAccount.getAccountNumber(), toAccountNo, amount, transactedAt);
	}

	private TransactionDirection toTransactionDirection(String code) {
		return DEPOSIT_CODE.equals(code) ? TransactionDirection.DEPOSIT : TransactionDirection.WITHDRAWAL;
	}

	private OffsetDateTime toTransactedAt(String date, String time) {
		return LocalDateTime.parse(date + time, TRANSACTED_AT_FORMATTER).atZone(KST).toOffsetDateTime();
	}
}
