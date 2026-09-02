package com.bana.dotori.account.service;

import com.bana.dotori.account.dto.AccountAuthRequest;
import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountResponse;

public interface AccountService {

	// 1원 송금 인증을 요청한다
	AccountAuthResponse requestAuth(Integer userId, AccountAuthRequest request);

	// 인증코드를 검증하고 계좌를 연동한다
	AccountResponse verifyAuth(Integer userId, AccountAuthVerifyRequest request);
}
