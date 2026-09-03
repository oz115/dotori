package com.bana.dotori.account.service;

import com.bana.dotori.account.dto.AccountAuthRequest;
import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.dto.AccountTransactionHistoryResponse;

public interface AccountService {

	// 1원 송금 인증을 요청한다
	AccountAuthResponse requestAuth(Integer userId, AccountAuthRequest request);

	// 인증코드를 검증하고 계좌를 연동한다
	AccountResponse verifyAuth(Integer userId, AccountAuthVerifyRequest request);

	// 연동 계좌의 거래내역을 커서 기반으로 조회한다
	AccountTransactionHistoryResponse getTransactionHistory(Integer userId, Long accountId, String cursor,
		Integer size, String transactionType);
}
