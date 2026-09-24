package com.bana.dotori.finance.client;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.bana.dotori.finance.config.FinanceProperties;
import com.bana.dotori.finance.dto.account.InquireDemandDepositAccountRec;
import com.bana.dotori.finance.dto.account.InquireDemandDepositAccountRequest;
import com.bana.dotori.finance.dto.account.InquireDemandDepositAccountResponse;
import com.bana.dotori.finance.dto.auth.CheckAuthCodeRequest;
import com.bana.dotori.finance.dto.auth.CheckAuthCodeResponse;
import com.bana.dotori.finance.dto.auth.OpenAccountAuthRequest;
import com.bana.dotori.finance.dto.auth.OpenAccountAuthResponse;
import com.bana.dotori.finance.dto.common.FinanceCommonHeader;
import com.bana.dotori.finance.dto.common.FinanceResponseHeader;
import com.bana.dotori.finance.dto.member.SearchMemberRequest;
import com.bana.dotori.finance.dto.member.SearchMemberResponse;
import com.bana.dotori.finance.dto.transaction.InquireTransactionHistoryListRec;
import com.bana.dotori.finance.dto.transaction.InquireTransactionHistoryListRequest;
import com.bana.dotori.finance.dto.transaction.InquireTransactionHistoryListResponse;
import com.bana.dotori.finance.dto.transfer.TransferAccountRec;
import com.bana.dotori.finance.dto.transfer.UpdateDemandDepositAccountTransferRequest;
import com.bana.dotori.finance.dto.transfer.UpdateDemandDepositAccountTransferResponse;
import com.bana.dotori.finance.exception.FinanceApiException;
import com.bana.dotori.finance.util.FinanceHeaderGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientFinanceClient implements FinanceClient {

	private static final String API_OPEN_ACCOUNT_AUTH = "openAccountAuth";
	private static final String API_CHECK_AUTH_CODE = "checkAuthCode";
	private static final String API_INQUIRE_TRANSACTION_HISTORY_LIST = "inquireTransactionHistoryList";
	private static final String API_INQUIRE_DEMAND_DEPOSIT_ACCOUNT = "inquireDemandDepositAccount";
	private static final String API_UPDATE_DEMAND_DEPOSIT_ACCOUNT_TRANSFER = "updateDemandDepositAccountTransfer";

	private final RestClient financeRestClient;
	private final FinanceProperties properties;
	private final FinanceHeaderGenerator headerGenerator;

	// 금융망에서 회원의 userKey를 조회한다
	@Override
	public String getUserKey(String userId) {
		try {
			SearchMemberResponse response = financeRestClient.post()
				.uri("/member/search")
				.body(new SearchMemberRequest(properties.apiKey(), userId))
				.retrieve()
				.body(SearchMemberResponse.class);

			if (response == null || response.userKey() == null || response.userKey().isBlank()) {
				throw new FinanceApiException("금융망 회원 조회 응답에 userKey가 존재하지 않습니다");
			}
			return response.userKey();
		} catch (RestClientResponseException ex) {
			log.error("금융망 회원 조회 실패({}): {}", ex.getStatusCode(), ex.getMessage());
			throw new FinanceApiException("금융망에 등록되지 않은 회원입니다", ex);
		} catch (RestClientException ex) {
			log.error("금융망 회원 조회 중 통신 오류가 발생했습니다: {}", ex.getMessage());
			throw new FinanceApiException("금융망 회원 조회 중 통신 오류가 발생했습니다", ex);
		}
	}

	// 대상 계좌로 1원 송금 인증을 요청한다
	@Override
	public void openAccountAuth(String userKey, String accountNo, String authText) {
		FinanceCommonHeader header = headerGenerator.createHeader(API_OPEN_ACCOUNT_AUTH, userKey);
		OpenAccountAuthRequest request = OpenAccountAuthRequest.builder()
			.header(header)
			.accountNo(accountNo)
			.authText(authText)
			.build();

		try {
			OpenAccountAuthResponse response = financeRestClient.post()
				.uri("/edu/accountAuth/openAccountAuth")
				.body(request)
				.retrieve()
				.body(OpenAccountAuthResponse.class);

			validateResponse(response != null ? response.header() : null, "1원 송금 요청");
		} catch (FinanceApiException ex) {
			throw ex;
		} catch (RestClientException ex) {
			log.error("금융망 1원 송금 요청 실패: {}", ex.getMessage());
			throw new FinanceApiException("금융망 1원 송금 요청 중 통신 오류가 발생했습니다", ex);
		}
	}

	// 1원 송금 인증코드를 검증한다
	@Override
	public void checkAuthCode(String userKey, String accountNo, String authText, String authCode) {
		FinanceCommonHeader header = headerGenerator.createHeader(API_CHECK_AUTH_CODE, userKey);
		CheckAuthCodeRequest request = CheckAuthCodeRequest.builder()
			.header(header)
			.accountNo(accountNo)
			.authText(authText)
			.authCode(authCode)
			.build();

		try {
			CheckAuthCodeResponse response = financeRestClient.post()
				.uri("/edu/accountAuth/checkAuthCode")
				.body(request)
				.retrieve()
				.body(CheckAuthCodeResponse.class);

			validateResponse(response != null ? response.header() : null, "1원 인증코드 검증");
		} catch (FinanceApiException ex) {
			throw ex;
		} catch (RestClientException ex) {
			log.error("금융망 1원 인증코드 검증 실패: {}", ex.getMessage());
			throw new FinanceApiException("금융망 1원 인증코드 검증 중 통신 오류가 발생했습니다", ex);
		}
	}

	// 연동 계좌의 거래내역을 조회한다
	@Override
	public InquireTransactionHistoryListRec inquireTransactionHistoryList(String userKey, String accountNo,
		String startDate, String endDate, String transactionType, String orderByType) {
		FinanceCommonHeader header = headerGenerator.createHeader(API_INQUIRE_TRANSACTION_HISTORY_LIST, userKey);
		InquireTransactionHistoryListRequest request = InquireTransactionHistoryListRequest.builder()
			.header(header)
			.accountNo(accountNo)
			.startDate(startDate)
			.endDate(endDate)
			.transactionType(transactionType)
			.orderByType(orderByType)
			.build();

		try {
			InquireTransactionHistoryListResponse response = financeRestClient.post()
				.uri("/edu/demandDeposit/inquireTransactionHistoryList")
				.body(request)
				.retrieve()
				.body(InquireTransactionHistoryListResponse.class);

			validateResponse(response != null ? response.header() : null, "거래내역 조회");
			return response.rec();
		} catch (FinanceApiException ex) {
			throw ex;
		} catch (RestClientException ex) {
			log.error("금융망 거래내역 조회 실패: {}", ex.getMessage());
			throw new FinanceApiException("금융망 거래내역 조회 중 통신 오류가 발생했습니다", ex);
		}
	}

	// 연동 계좌의 상세 정보를 조회한다
	@Override
	public InquireDemandDepositAccountRec inquireDemandDepositAccount(String userKey, String accountNo) {
		FinanceCommonHeader header = headerGenerator.createHeader(API_INQUIRE_DEMAND_DEPOSIT_ACCOUNT, userKey);
		InquireDemandDepositAccountRequest request = InquireDemandDepositAccountRequest.builder()
			.header(header)
			.accountNo(accountNo)
			.build();

		try {
			InquireDemandDepositAccountResponse response = financeRestClient.post()
				.uri("/edu/demandDeposit/inquireDemandDepositAccount")
				.body(request)
				.retrieve()
				.body(InquireDemandDepositAccountResponse.class);

			validateResponse(response != null ? response.header() : null, "계좌 정보 조회");
			return response.rec();
		} catch (FinanceApiException ex) {
			throw ex;
		} catch (RestClientException ex) {
			log.error("금융망 계좌 정보 조회 실패: {}", ex.getMessage());
			throw new FinanceApiException("금융망 계좌 정보 조회 중 통신 오류가 발생했습니다", ex);
		}
	}

	// 출금 계좌에서 입금 계좌로 이체한다
	@Override
	public List<TransferAccountRec> updateDemandDepositAccountTransfer(String userKey, String withdrawalAccountNo,
		String depositAccountNo, long amount, String withdrawalSummary, String depositSummary) {
		FinanceCommonHeader header = headerGenerator.createHeader(API_UPDATE_DEMAND_DEPOSIT_ACCOUNT_TRANSFER, userKey);
		UpdateDemandDepositAccountTransferRequest request = UpdateDemandDepositAccountTransferRequest.builder()
			.header(header)
			.depositAccountNo(depositAccountNo)
			.depositTransactionSummary(depositSummary)
			.transactionBalance(String.valueOf(amount))
			.withdrawalAccountNo(withdrawalAccountNo)
			.withdrawalTransactionSummary(withdrawalSummary)
			.build();

		try {
			UpdateDemandDepositAccountTransferResponse response = financeRestClient.post()
				.uri("/edu/demandDeposit/updateDemandDepositAccountTransfer")
				.body(request)
				.retrieve()
				.body(UpdateDemandDepositAccountTransferResponse.class);

			validateResponse(response != null ? response.header() : null, "계좌 이체");
			return response.rec();
		} catch (FinanceApiException ex) {
			throw ex;
		} catch (RestClientException ex) {
			log.error("금융망 계좌 이체 실패: {}", ex.getMessage());
			throw new FinanceApiException("금융망 계좌 이체 중 통신 오류가 발생했습니다", ex);
		}
	}

	// 응답 헤더의 성공 여부를 검증한다
	private void validateResponse(FinanceResponseHeader header, String operation) {
		if (header == null) {
			throw new FinanceApiException(operation + " 응답 헤더가 비어 있습니다");
		}
		if (!header.isSuccess()) {
			log.warn("금융망 {} 실패 - code: {}, message: {}", operation, header.responseCode(), header.responseMessage());
			throw new FinanceApiException(header.responseCode(), header.responseMessage());
		}
	}
}
