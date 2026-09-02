package com.bana.dotori.account.service;

import com.bana.dotori.account.dto.AccountAuthRequest;
import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountResponse;

public interface AccountService {

	AccountAuthResponse requestAuth(Integer userId, AccountAuthRequest request);

	AccountResponse verifyAuth(Integer userId, AccountAuthVerifyRequest request);
}
