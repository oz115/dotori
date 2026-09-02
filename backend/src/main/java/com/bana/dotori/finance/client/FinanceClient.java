package com.bana.dotori.finance.client;

public interface FinanceClient {

	String getOrCreateUserKey(String userId);

	void openAccountAuth(String userKey, String accountNo, String authText);

	void checkAuthCode(String userKey, String accountNo, String authText, String authCode);
}
