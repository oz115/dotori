package com.bana.dotori.finance.client;

public interface FinanceClient {

	// 금융망에서 회원의 userKey를 조회한다
	String getUserKey(String userId);

	// 대상 계좌로 1원 송금 인증을 요청한다
	void openAccountAuth(String userKey, String accountNo, String authText);

	// 1원 송금 인증코드를 검증한다
	void checkAuthCode(String userKey, String accountNo, String authText, String authCode);
}
