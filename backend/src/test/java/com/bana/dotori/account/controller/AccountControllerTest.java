package com.bana.dotori.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.account.dto.AccountAuthRequest;
import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountInfoResponse;
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.dto.AccountTransactionHistoryResponse;
import com.bana.dotori.account.dto.AccountTransactionResponse;
import com.bana.dotori.account.dto.TransactionDirection;
import com.bana.dotori.account.service.AccountService;
import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class AccountControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AccountService accountService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private static final String AUTH_URL = "/api/v1/accounts/auth";
	private static final String VERIFY_URL = "/api/v1/accounts/auth/verify";
	private static final String ME_URL = "/api/v1/accounts/me";
	private static final String TRANSACTIONS_URL = "/api/v1/accounts/transactions";
	private static final String ACCESS_TOKEN = "valid-access-token";

	private static final String AUTH_BODY = """
		{
			"bankCode": "003",
			"bankName": "국민은행",
			"accountNo": "0011214764051239"
		}
		""";

	private static final String VERIFY_BODY = """
		{
			"bankCode": "003",
			"bankName": "국민은행",
			"accountNo": "0011214764051239",
			"authCode": "1234"
		}
		""";

	private void authenticateAs(Integer userId) {
		given(jwtTokenProvider.getAuthentication(ACCESS_TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));
	}

	@Test
	@DisplayName("계좌 인증 요청 성공 시 200과 계좌번호를 반환한다")
	void requestAuth_success() throws Exception {
		authenticateAs(1);
		given(accountService.requestAuth(eq(1), any(AccountAuthRequest.class)))
			.willReturn(new AccountAuthResponse("0011214764051239", "도토리", 180L));

		mockMvc.perform(post(AUTH_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(AUTH_BODY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountNo").value("0011214764051239"))
			.andExpect(jsonPath("$.authText").value("도토리"))
			.andExpect(jsonPath("$.expiresInSeconds").value(180));
	}

	@Test
	@DisplayName("필수 값이 누락되면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void requestAuth_invalidRequest() throws Exception {
		authenticateAs(1);

		mockMvc.perform(post(AUTH_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"bankCode\": \"\", \"bankName\": \"국민은행\", \"accountNo\": \"0011214764051239\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(accountService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("인증 헤더 없이 요청하면 401을 반환한다")
	void requestAuth_withoutAuth() throws Exception {
		mockMvc.perform(post(AUTH_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(AUTH_BODY))
			.andExpect(status().isUnauthorized());

		then(accountService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("이미 연동된 계좌면 409와 ACCOUNT_ALREADY_LINKED 에러를 반환한다")
	void requestAuth_alreadyLinked() throws Exception {
		authenticateAs(1);
		given(accountService.requestAuth(eq(1), any(AccountAuthRequest.class)))
			.willThrow(new BusinessException(ErrorCode.ACCOUNT_ALREADY_LINKED));

		mockMvc.perform(post(AUTH_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(AUTH_BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ACCOUNT_ALREADY_LINKED"));
	}

	@Test
	@DisplayName("인증코드 검증 성공 시 201과 연동된 계좌 정보를 반환한다")
	void verifyAuth_success() throws Exception {
		authenticateAs(1);
		given(accountService.verifyAuth(eq(1), any(AccountAuthVerifyRequest.class)))
			.willReturn(new AccountResponse(1, "003", "국민은행", "0011214764051239", OffsetDateTime.now()));

		mockMvc.perform(post(VERIFY_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(VERIFY_BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.bankCode").value("003"))
			.andExpect(jsonPath("$.accountNo").value("0011214764051239"));
	}

	@Test
	@DisplayName("필수 값이 누락되면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void verifyAuth_invalidRequest() throws Exception {
		authenticateAs(1);

		mockMvc.perform(post(VERIFY_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"bankCode\": \"003\", \"bankName\": \"국민은행\", \"accountNo\": \"0011214764051239\", "
					+ "\"authCode\": \"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(accountService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("인증코드가 일치하지 않으면 400과 AUTH_CODE_MISMATCH 에러를 반환한다")
	void verifyAuth_codeMismatch() throws Exception {
		authenticateAs(1);
		given(accountService.verifyAuth(eq(1), any(AccountAuthVerifyRequest.class)))
			.willThrow(new BusinessException(ErrorCode.AUTH_CODE_MISMATCH));

		mockMvc.perform(post(VERIFY_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(VERIFY_BODY))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AUTH_CODE_MISMATCH"));
	}

	@Test
	@DisplayName("인증 시도 횟수를 초과하면 400과 AUTH_ATTEMPT_EXCEEDED 에러를 반환한다")
	void verifyAuth_attemptExceeded() throws Exception {
		authenticateAs(1);
		given(accountService.verifyAuth(eq(1), any(AccountAuthVerifyRequest.class)))
			.willThrow(new BusinessException(ErrorCode.AUTH_ATTEMPT_EXCEEDED));

		mockMvc.perform(post(VERIFY_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(VERIFY_BODY))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AUTH_ATTEMPT_EXCEEDED"));
	}

	@Test
	@DisplayName("인증 세션이 없으면 400과 AUTH_SESSION_NOT_FOUND 에러를 반환한다")
	void verifyAuth_sessionNotFound() throws Exception {
		authenticateAs(1);
		given(accountService.verifyAuth(eq(1), any(AccountAuthVerifyRequest.class)))
			.willThrow(new BusinessException(ErrorCode.AUTH_SESSION_NOT_FOUND));

		mockMvc.perform(post(VERIFY_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(VERIFY_BODY))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AUTH_SESSION_NOT_FOUND"));
	}

	@Test
	@DisplayName("인증 헤더 없이 검증을 요청하면 401을 반환한다")
	void verifyAuth_withoutAuth() throws Exception {
		mockMvc.perform(post(VERIFY_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(VERIFY_BODY))
			.andExpect(status().isUnauthorized());

		then(accountService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("계좌 정보 조회 성공 시 200과 계좌 상세 정보를 반환한다")
	void getAccountInfo_success() throws Exception {
		authenticateAs(1);
		given(accountService.getAccountInfo(1)).willReturn(
			new AccountInfoResponse("003", "국민은행", "0011214764051239", "국민은행 수시입출금 상품명", "수시입출금", 50000L,
				100000000L, 20000000L, "KRW"));

		mockMvc.perform(get(ME_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.bankCode").value("003"))
			.andExpect(jsonPath("$.bankName").value("국민은행"))
			.andExpect(jsonPath("$.accountNo").value("0011214764051239"))
			.andExpect(jsonPath("$.accountBalance").value(50000))
			.andExpect(jsonPath("$.currency").value("KRW"));
	}

	@Test
	@DisplayName("연동된 계좌가 없으면 404와 ACCOUNT_NOT_LINKED 에러를 반환한다")
	void getAccountInfo_notLinked() throws Exception {
		authenticateAs(1);
		given(accountService.getAccountInfo(1)).willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED));

		mockMvc.perform(get(ME_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_LINKED"));
	}

	@Test
	@DisplayName("인증 헤더 없이 계좌 정보를 요청하면 401을 반환한다")
	void getAccountInfo_withoutAuth() throws Exception {
		mockMvc.perform(get(ME_URL))
			.andExpect(status().isUnauthorized());

		then(accountService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("거래내역 조회 성공 시 200과 거래내역·다음 커서를 반환한다")
	void getTransactionHistory_success() throws Exception {
		authenticateAs(1);
		AccountTransactionResponse transaction = new AccountTransactionResponse("61", TransactionDirection.WITHDRAWAL,
			"출금(이체)", 10000000, 89900000, "(수시입출금) : 출금(이체)", OffsetDateTime.now());
		given(accountService.getTransactionHistory(eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
			.willReturn(new AccountTransactionHistoryResponse(List.of(transaction), "61", true));

		mockMvc.perform(get(TRANSACTIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.transactions[0].transactionUniqueNo").value("61"))
			.andExpect(jsonPath("$.transactions[0].transactionType").value("WITHDRAWAL"))
			.andExpect(jsonPath("$.nextCursor").value("61"))
			.andExpect(jsonPath("$.hasNext").value(true));
	}

	@Test
	@DisplayName("거래내역 조회 시 cursor/size 파라미터를 그대로 서비스에 전달한다")
	void getTransactionHistory_withCursorAndSize() throws Exception {
		authenticateAs(1);
		given(accountService.getTransactionHistory(eq(1), isNull(), isNull(), isNull(), eq("60"), eq(10),
			anyString())).willReturn(new AccountTransactionHistoryResponse(List.of(), null, false));

		mockMvc.perform(get(TRANSACTIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.param("cursor", "60")
				.param("size", "10")
				.param("transactionType", "DEPOSIT"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.transactions").isEmpty())
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	@DisplayName("거래내역 조회 시 period 파라미터를 그대로 서비스에 전달한다")
	void getTransactionHistory_withPeriod() throws Exception {
		authenticateAs(1);
		given(accountService.getTransactionHistory(eq(1), eq("WEEK"), isNull(), isNull(), isNull(), isNull(),
			isNull())).willReturn(new AccountTransactionHistoryResponse(List.of(), null, false));

		mockMvc.perform(get(TRANSACTIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.param("period", "WEEK"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("거래내역 조회 시 startDate/endDate 파라미터를 LocalDate로 바인딩해 서비스에 전달한다")
	void getTransactionHistory_withDateRange() throws Exception {
		authenticateAs(1);
		given(accountService.getTransactionHistory(eq(1), isNull(), eq(LocalDate.of(2024, 1, 1)),
			eq(LocalDate.of(2024, 3, 1)), isNull(), isNull(), isNull()))
			.willReturn(new AccountTransactionHistoryResponse(List.of(), null, false));

		mockMvc.perform(get(TRANSACTIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.param("startDate", "2024-01-01")
				.param("endDate", "2024-03-01"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("조회 기간이 3개월을 초과하면 400과 INVALID_REQUEST 에러를 반환한다")
	void getTransactionHistory_기간초과() throws Exception {
		authenticateAs(1);
		given(accountService.getTransactionHistory(eq(1), isNull(), eq(LocalDate.of(2024, 1, 1)),
			eq(LocalDate.of(2024, 5, 1)), isNull(), isNull(), isNull()))
			.willThrow(new BusinessException(ErrorCode.INVALID_REQUEST));

		mockMvc.perform(get(TRANSACTIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
				.param("startDate", "2024-01-01")
				.param("endDate", "2024-05-01"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("연동된 계좌가 없으면 404와 ACCOUNT_NOT_LINKED 에러를 반환한다")
	void getTransactionHistory_notLinked() throws Exception {
		authenticateAs(1);
		given(accountService.getTransactionHistory(eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
			.willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED));

		mockMvc.perform(get(TRANSACTIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_LINKED"));
	}

	@Test
	@DisplayName("인증 헤더 없이 거래내역을 요청하면 401을 반환한다")
	void getTransactionHistory_withoutAuth() throws Exception {
		mockMvc.perform(get(TRANSACTIONS_URL))
			.andExpect(status().isUnauthorized());

		then(accountService).shouldHaveNoInteractions();
	}
}
