package com.bana.dotori.account.service;

import java.time.LocalDate;

import com.bana.dotori.account.dto.AccountAuthRequest;
import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountInfoResponse;
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.dto.AccountTransactionHistoryResponse;
import com.bana.dotori.account.dto.TransferRequest;
import com.bana.dotori.account.dto.TransferResponse;

public interface AccountService {

	// 1원 송금 인증을 요청한다
	AccountAuthResponse requestAuth(Integer userId, AccountAuthRequest request);

	// 인증코드를 검증하고 계좌를 연동한다
	AccountResponse verifyAuth(Integer userId, AccountAuthVerifyRequest request);

	// 연동 계좌의 상세 정보를 금융망에서 실시간으로 조회한다
	AccountInfoResponse getAccountInfo(Integer userId);

	// 연동 계좌의 거래내역을 커서 기반으로 조회한다.
	// period(상대 기간) 또는 startDate/endDate(절대 기간) 중 하나만 지정할 수 있고, 조회 기간은 최대 3개월이다
	AccountTransactionHistoryResponse getTransactionHistory(Integer userId, String period, LocalDate startDate,
		LocalDate endDate, String cursor, Integer size, String transactionType);

	// 연동 계좌 간 이체를 수행한다
	TransferResponse transfer(Integer userId, TransferRequest request, String idempotencyKey);

	// 연동 계좌 간 이체를 수행하며, 받는 계좌에 표시될 적요를 직접 지정한다 (null/공백이면 기본 적요)
	TransferResponse transfer(Integer userId, TransferRequest request, String idempotencyKey, String depositSummary);

	// 연동 계좌번호만 조회한다 (금융망 호출 없이 로컬 DB만 본다)
	String getLinkedAccountNumber(Integer userId);

	// 연동된 계좌를 해제한다
	void unlinkAccount(Integer userId);
}
