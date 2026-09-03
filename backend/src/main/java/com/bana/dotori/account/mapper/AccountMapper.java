package com.bana.dotori.account.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.entity.Account;
import com.bana.dotori.user.entity.User;

@Component
public class AccountMapper {

	public AccountAuthResponse toAccountAuthResponse(String accountNo) {
		return new AccountAuthResponse(accountNo);
	}

	public Account toAccount(User user, AccountAuthVerifyRequest request, String financeUserKey) {
		return Account.builder()
			.user(user)
			.bankCode(request.bankCode())
			.bankName(request.bankName())
			.accountNumber(request.accountNo())
			.financeUserKey(financeUserKey)
			.build();
	}

	public AccountResponse toAccountResponse(Account account) {
		return new AccountResponse(account.getId(), account.getBankCode(), account.getBankName(),
			account.getAccountNumber(), account.getLinkedAt());
	}
}
