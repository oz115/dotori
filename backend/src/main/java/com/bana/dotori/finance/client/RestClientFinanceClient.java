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
import com.bana.dotori.finance.dto.member.CreateMemberRequest;
import com.bana.dotori.finance.dto.member.CreateMemberResponse;
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

	@Override
	public String getOrCreateUserKey(String userId) {
		try {
			SearchMemberResponse response = financeRestClient.post()
				.uri("/member/search")
				.body(new SearchMemberRequest(properties.apiKey(), userId))
				.retrieve()
				.body(SearchMemberResponse.class);

			if (response != null && response.userKey() != null && !response.userKey().isBlank()) {
				return response.userKey();
			}
		} catch (RestClientResponseException ex) {
			log.info("금융망 회원 조회 실패({}), 신규 회원 등록을 진행합니다.", ex.getStatusCode());
		} catch (RestClientException ex) {
			log.warn("금융망 회원 조회 중 예외 발생, 신규 회원 등록을 시도합니다: {}", ex.getMessage());
		}

		return createMember(userId);
	}

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

	private String createMember(String userId) {
		try {
			CreateMemberResponse response = financeRestClient.post()
				.uri("/member")
				.body(new CreateMemberRequest(properties.apiKey(), userId))
				.retrieve()
				.body(CreateMemberResponse.class);

			if (response == null || response.userKey() == null || response.userKey().isBlank()) {
				throw new FinanceApiException("금융망 회원 등록 응답에 userKey가 존재하지 않습니다");
			}
			return response.userKey();
		} catch (RestClientException ex) {
			log.error("금융망 회원 등록 실패: {}", ex.getMessage());
			throw new FinanceApiException("금융망 회원 등록 중 통신 오류가 발생했습니다", ex);
		}
	}

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
