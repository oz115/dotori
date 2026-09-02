package com.bana.dotori.finance.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.bana.dotori.finance.config.FinanceProperties;
import com.bana.dotori.finance.dto.auth.CheckAuthCodeRequest;
import com.bana.dotori.finance.dto.auth.CheckAuthCodeResponse;
import com.bana.dotori.finance.dto.auth.OpenAccountAuthRequest;
import com.bana.dotori.finance.dto.auth.OpenAccountAuthResponse;
import com.bana.dotori.finance.dto.common.FinanceCommonHeader;
import com.bana.dotori.finance.dto.common.FinanceResponseHeader;
import com.bana.dotori.finance.dto.member.SearchMemberRequest;
import com.bana.dotori.finance.dto.member.SearchMemberResponse;
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
