package com.bana.dotori.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.account.dto.AccountAuthRequest;
import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountInfoResponse;
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.dto.AccountTransactionHistoryResponse;
import com.bana.dotori.account.dto.TransactionDirection;
import com.bana.dotori.account.entity.Account;
import com.bana.dotori.account.mapper.AccountMapper;
import com.bana.dotori.account.repository.AccountRepository;
import com.bana.dotori.finance.client.FinanceClient;
import com.bana.dotori.finance.config.FinanceProperties;
import com.bana.dotori.finance.dto.account.InquireDemandDepositAccountRec;
import com.bana.dotori.finance.dto.transaction.InquireTransactionHistoryListRec;
import com.bana.dotori.finance.dto.transaction.TransactionHistoryItem;
import com.bana.dotori.finance.exception.FinanceApiException;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private FinanceClient financeClient;

	@Mock
	private AccountAuthSessionManager accountAuthSessionManager;

	@Spy
	private AccountMapper accountMapper;

	private final FinanceProperties financeProperties =
		new FinanceProperties("https://finopenapi.ssafy.io/ssafy/api/v1", "test-api-key", "00100", "001", "도토리");

	private AccountService accountService;

	private static final Integer USER_ID = 1;
	private static final String ACCOUNT_NO = "0011214764051239";
	private static final String USER_KEY = "633d65a0-67c4-48b2-9bbf-94b948d8e141";

	@BeforeEach
	void setUp() {
		accountService = new AccountServiceImpl(accountRepository, userRepository, financeClient,
			financeProperties, accountAuthSessionManager, accountMapper);
	}

	private User user(Integer id) {
		User user = User.builder()
			.loginId("dotori123")
			.password("encoded-password")
			.name("김도토")
			.email("dotori@example.com")
			.role(UserRole.PARENT)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Account account(Long id) {
		Account account = Account.builder()
			.user(user(USER_ID))
			.bankCode("003")
			.bankName("국민은행")
			.accountNumber(ACCOUNT_NO)
			.financeUserKey(USER_KEY)
			.build();
		ReflectionTestUtils.setField(account, "id", id);
		return account;
	}

	private TransactionHistoryItem transactionItem(String uniqueNo, String type, String typeName, String balance,
		String afterBalance) {
		return TransactionHistoryItem.builder()
			.transactionUniqueNo(uniqueNo)
			.transactionDate("20240401")
			.transactionTime("102447")
			.transactionType(type)
			.transactionTypeName(typeName)
			.transactionAccountNo("")
			.transactionBalance(balance)
			.transactionAfterBalance(afterBalance)
			.transactionSummary("(수시입출금) : " + typeName)
			.transactionMemo("")
			.build();
	}

	@Test
	@DisplayName("requestAuth_성공시_1원_송금을_요청하고_인증_세션을_생성한다")
	void requestAuth_success() {
		given(accountRepository.existsByUserIdAndAccountNumber(USER_ID, ACCOUNT_NO)).willReturn(false);
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
		given(financeClient.getUserKey("dotori@example.com")).willReturn(USER_KEY);
		given(accountAuthSessionManager.sessionTtlSeconds()).willReturn(180L);

		AccountAuthResponse response = accountService.requestAuth(USER_ID,
			new AccountAuthRequest("003", "국민은행", ACCOUNT_NO));

		assertThat(response.accountNo()).isEqualTo(ACCOUNT_NO);
		assertThat(response.authText()).isEqualTo("도토리");
		assertThat(response.expiresInSeconds()).isEqualTo(180L);
		then(financeClient).should().openAccountAuth(USER_KEY, ACCOUNT_NO, "도토리");
		then(accountAuthSessionManager).should().create(USER_ID, ACCOUNT_NO, USER_KEY);
	}

	@Test
	@DisplayName("requestAuth_이미_연동된_계좌면_ACCOUNT_ALREADY_LINKED_예외가_발생한다")
	void requestAuth_alreadyLinked() {
		given(accountRepository.existsByUserIdAndAccountNumber(USER_ID, ACCOUNT_NO)).willReturn(true);

		assertThatThrownBy(() -> accountService.requestAuth(USER_ID,
			new AccountAuthRequest("003", "국민은행", ACCOUNT_NO)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.ACCOUNT_ALREADY_LINKED);

		then(financeClient).shouldHaveNoInteractions();
		then(accountAuthSessionManager).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("requestAuth_존재하지_않는_유저면_NOT_FOUND_예외가_발생한다")
	void requestAuth_userNotFound() {
		given(accountRepository.existsByUserIdAndAccountNumber(USER_ID, ACCOUNT_NO)).willReturn(false);
		given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.requestAuth(USER_ID,
			new AccountAuthRequest("003", "국민은행", ACCOUNT_NO)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("requestAuth_금융망_오류시_FINANCE_API_ERROR_예외가_발생하고_세션은_생성되지_않는다")
	void requestAuth_financeApiException() {
		given(accountRepository.existsByUserIdAndAccountNumber(USER_ID, ACCOUNT_NO)).willReturn(false);
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
		given(financeClient.getUserKey("dotori@example.com")).willReturn(USER_KEY);
		willThrow(new FinanceApiException("A1001", "존재하지 않는 계좌번호입니다."))
			.given(financeClient).openAccountAuth(USER_KEY, ACCOUNT_NO, "도토리");

		assertThatThrownBy(() -> accountService.requestAuth(USER_ID,
			new AccountAuthRequest("003", "국민은행", ACCOUNT_NO)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FINANCE_API_ERROR);

		then(accountAuthSessionManager).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("verifyAuth_성공시_세션을_삭제하고_계좌를_저장한다")
	void verifyAuth_success() {
		given(accountRepository.existsByUserIdAndAccountNumber(USER_ID, ACCOUNT_NO)).willReturn(false);
		given(accountAuthSessionManager.getUserKey(USER_ID, ACCOUNT_NO)).willReturn(USER_KEY);
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
		given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

		AccountResponse response = accountService.verifyAuth(USER_ID,
			new AccountAuthVerifyRequest("003", "국민은행", ACCOUNT_NO, "1234"));

		assertThat(response.bankCode()).isEqualTo("003");
		assertThat(response.bankName()).isEqualTo("국민은행");
		assertThat(response.accountNo()).isEqualTo(ACCOUNT_NO);
		then(financeClient).should().checkAuthCode(USER_KEY, ACCOUNT_NO, "도토리", "1234");
		then(accountAuthSessionManager).should().delete(USER_ID, ACCOUNT_NO);
	}

	@Test
	@DisplayName("verifyAuth_이미_연동된_계좌면_ACCOUNT_ALREADY_LINKED_예외가_발생한다")
	void verifyAuth_alreadyLinked() {
		given(accountRepository.existsByUserIdAndAccountNumber(USER_ID, ACCOUNT_NO)).willReturn(true);

		assertThatThrownBy(() -> accountService.verifyAuth(USER_ID,
			new AccountAuthVerifyRequest("003", "국민은행", ACCOUNT_NO, "1234")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.ACCOUNT_ALREADY_LINKED);

		then(accountAuthSessionManager).shouldHaveNoInteractions();
		then(financeClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("verifyAuth_인증_세션이_없으면_AUTH_SESSION_NOT_FOUND_예외가_그대로_전파된다")
	void verifyAuth_sessionNotFound() {
		given(accountRepository.existsByUserIdAndAccountNumber(USER_ID, ACCOUNT_NO)).willReturn(false);
		given(accountAuthSessionManager.getUserKey(USER_ID, ACCOUNT_NO))
			.willThrow(new BusinessException(ErrorCode.AUTH_SESSION_NOT_FOUND));

		assertThatThrownBy(() -> accountService.verifyAuth(USER_ID,
			new AccountAuthVerifyRequest("003", "국민은행", ACCOUNT_NO, "1234")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.AUTH_SESSION_NOT_FOUND);

		then(financeClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("verifyAuth_인증코드가_일치하지_않으면_시도횟수를_증가시키고_AUTH_CODE_MISMATCH_예외가_발생한다")
	void verifyAuth_codeMismatch() {
		given(accountRepository.existsByUserIdAndAccountNumber(USER_ID, ACCOUNT_NO)).willReturn(false);
		given(accountAuthSessionManager.getUserKey(USER_ID, ACCOUNT_NO)).willReturn(USER_KEY);
		willThrow(new FinanceApiException("A1002", "인증코드가 일치하지 않습니다."))
			.given(financeClient).checkAuthCode(USER_KEY, ACCOUNT_NO, "도토리", "1234");

		assertThatThrownBy(() -> accountService.verifyAuth(USER_ID,
			new AccountAuthVerifyRequest("003", "국민은행", ACCOUNT_NO, "1234")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.AUTH_CODE_MISMATCH);

		then(accountAuthSessionManager).should().incrementAttempt(USER_ID, ACCOUNT_NO);
		then(accountRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("verifyAuth_시도횟수_초과시_AUTH_ATTEMPT_EXCEEDED_예외가_그대로_전파된다")
	void verifyAuth_attemptExceeded() {
		given(accountRepository.existsByUserIdAndAccountNumber(USER_ID, ACCOUNT_NO)).willReturn(false);
		given(accountAuthSessionManager.getUserKey(USER_ID, ACCOUNT_NO)).willReturn(USER_KEY);
		willThrow(new FinanceApiException("A1002", "인증코드가 일치하지 않습니다."))
			.given(financeClient).checkAuthCode(USER_KEY, ACCOUNT_NO, "도토리", "1234");
		willThrow(new BusinessException(ErrorCode.AUTH_ATTEMPT_EXCEEDED))
			.given(accountAuthSessionManager).incrementAttempt(USER_ID, ACCOUNT_NO);

		assertThatThrownBy(() -> accountService.verifyAuth(USER_ID,
			new AccountAuthVerifyRequest("003", "국민은행", ACCOUNT_NO, "1234")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.AUTH_ATTEMPT_EXCEEDED);

		then(accountRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("getAccountInfo_연동된_계좌가_없으면_ACCOUNT_NOT_LINKED_예외가_발생한다")
	void getAccountInfo_연동계좌없음() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.getAccountInfo(USER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.ACCOUNT_NOT_LINKED);

		then(financeClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("getAccountInfo_성공시_금융망_조회_결과를_반환한다")
	void getAccountInfo_success() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));
		InquireDemandDepositAccountRec rec = InquireDemandDepositAccountRec.builder()
			.bankCode("003")
			.bankName("국민은행")
			.userName("김도토")
			.accountNo(ACCOUNT_NO)
			.accountName("국민은행 수시입출금 상품명")
			.accountTypeCode("1")
			.accountTypeName("수시입출금")
			.accountCreatedDate("20240401")
			.accountExpiryDate("20290401")
			.dailyTransferLimit("100000000")
			.oneTimeTransferLimit("20000000")
			.accountBalance("50000")
			.lastTransactionDate("")
			.currency("KRW")
			.build();
		given(financeClient.inquireDemandDepositAccount(USER_KEY, ACCOUNT_NO)).willReturn(rec);

		AccountInfoResponse response = accountService.getAccountInfo(USER_ID);

		assertThat(response.bankCode()).isEqualTo("003");
		assertThat(response.bankName()).isEqualTo("국민은행");
		assertThat(response.accountNo()).isEqualTo(ACCOUNT_NO);
		assertThat(response.accountName()).isEqualTo("국민은행 수시입출금 상품명");
		assertThat(response.accountTypeName()).isEqualTo("수시입출금");
		assertThat(response.accountBalance()).isEqualTo(50000L);
		assertThat(response.dailyTransferLimit()).isEqualTo(100000000L);
		assertThat(response.oneTimeTransferLimit()).isEqualTo(20000000L);
		assertThat(response.currency()).isEqualTo("KRW");
	}

	@Test
	@DisplayName("getAccountInfo_금융망_오류시_FINANCE_API_ERROR_예외가_발생한다")
	void getAccountInfo_금융망오류() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));
		willThrow(new FinanceApiException("A1003", "계좌번호가 유효하지 않습니다."))
			.given(financeClient).inquireDemandDepositAccount(USER_KEY, ACCOUNT_NO);

		assertThatThrownBy(() -> accountService.getAccountInfo(USER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FINANCE_API_ERROR);
	}

	@Test
	@DisplayName("getTransactionHistory_연동된_계좌가_없으면_ACCOUNT_NOT_LINKED_예외가_발생한다")
	void getTransactionHistory_연동계좌없음() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(
			() -> accountService.getTransactionHistory(USER_ID, null, null, null, null, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.ACCOUNT_NOT_LINKED);

		then(financeClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("getTransactionHistory_커서가_없으면_최신_거래부터_size만큼_반환하고_다음_커서를_내려준다")
	void getTransactionHistory_첫페이지() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));
		InquireTransactionHistoryListRec rec = InquireTransactionHistoryListRec.builder()
			.totalCount("3")
			.list(List.of(
				transactionItem("61", "2", "출금(이체)", "10000000", "89900000"),
				transactionItem("60", "2", "출금", "100000", "99900000"),
				transactionItem("59", "1", "입금", "100000000", "100000000")))
			.build();
		given(financeClient.inquireTransactionHistoryList(eq(USER_KEY), eq(ACCOUNT_NO), anyString(), anyString(),
			eq("A"), eq("DESC"))).willReturn(rec);

		AccountTransactionHistoryResponse response =
			accountService.getTransactionHistory(USER_ID, null, null, null, null, 2, null);

		assertThat(response.transactions()).hasSize(2);
		assertThat(response.transactions().get(0).transactionUniqueNo()).isEqualTo("61");
		assertThat(response.transactions().get(0).transactionType()).isEqualTo(TransactionDirection.WITHDRAWAL);
		assertThat(response.transactions().get(1).transactionUniqueNo()).isEqualTo("60");
		assertThat(response.hasNext()).isTrue();
		assertThat(response.nextCursor()).isEqualTo("60");
	}

	@Test
	@DisplayName("getTransactionHistory_커서_이후_거래가_size보다_적으면_hasNext가_false다")
	void getTransactionHistory_마지막페이지() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));
		InquireTransactionHistoryListRec rec = InquireTransactionHistoryListRec.builder()
			.totalCount("3")
			.list(List.of(
				transactionItem("61", "2", "출금(이체)", "10000000", "89900000"),
				transactionItem("60", "2", "출금", "100000", "99900000"),
				transactionItem("59", "1", "입금", "100000000", "100000000")))
			.build();
		given(financeClient.inquireTransactionHistoryList(eq(USER_KEY), eq(ACCOUNT_NO), anyString(), anyString(),
			eq("A"), eq("DESC"))).willReturn(rec);

		AccountTransactionHistoryResponse response =
			accountService.getTransactionHistory(USER_ID, null, null, null, "60", 2, null);

		assertThat(response.transactions()).hasSize(1);
		assertThat(response.transactions().get(0).transactionUniqueNo()).isEqualTo("59");
		assertThat(response.transactions().get(0).transactionType()).isEqualTo(TransactionDirection.DEPOSIT);
		assertThat(response.hasNext()).isFalse();
		assertThat(response.nextCursor()).isNull();
	}

	@Test
	@DisplayName("getTransactionHistory_금융망_오류시_FINANCE_API_ERROR_예외가_발생한다")
	void getTransactionHistory_금융망오류() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));
		willThrow(new FinanceApiException("A1003", "계좌번호가 유효하지 않습니다."))
			.given(financeClient).inquireTransactionHistoryList(eq(USER_KEY), eq(ACCOUNT_NO), anyString(),
				anyString(), eq("A"), eq("DESC"));

		assertThatThrownBy(
			() -> accountService.getTransactionHistory(USER_ID, null, null, null, null, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FINANCE_API_ERROR);
	}

	@Test
	@DisplayName("getTransactionHistory_period가_WEEK이면_최근_1주_구간으로_조회한다")
	void getTransactionHistory_period_WEEK() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));
		InquireTransactionHistoryListRec rec = InquireTransactionHistoryListRec.builder()
			.totalCount("0")
			.list(List.of())
			.build();
		LocalDate today = LocalDate.now();
		String expectedStart = today.minusWeeks(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String expectedEnd = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		given(financeClient.inquireTransactionHistoryList(eq(USER_KEY), eq(ACCOUNT_NO), eq(expectedStart),
			eq(expectedEnd), eq("A"), eq("DESC"))).willReturn(rec);

		accountService.getTransactionHistory(USER_ID, "WEEK", null, null, null, null, null);

		then(financeClient).should()
			.inquireTransactionHistoryList(USER_KEY, ACCOUNT_NO, expectedStart, expectedEnd, "A", "DESC");
	}

	@Test
	@DisplayName("getTransactionHistory_startDate와_endDate를_지정하면_해당_절대_기간으로_조회한다")
	void getTransactionHistory_절대기간() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));
		InquireTransactionHistoryListRec rec = InquireTransactionHistoryListRec.builder()
			.totalCount("0")
			.list(List.of())
			.build();
		LocalDate startDate = LocalDate.of(2024, 1, 1);
		LocalDate endDate = LocalDate.of(2024, 3, 1);
		given(financeClient.inquireTransactionHistoryList(eq(USER_KEY), eq(ACCOUNT_NO), eq("20240101"),
			eq("20240301"), eq("A"), eq("DESC"))).willReturn(rec);

		accountService.getTransactionHistory(USER_ID, null, startDate, endDate, null, null, null);

		then(financeClient).should()
			.inquireTransactionHistoryList(USER_KEY, ACCOUNT_NO, "20240101", "20240301", "A", "DESC");
	}

	@Test
	@DisplayName("getTransactionHistory_period와_날짜를_함께_지정하면_INVALID_REQUEST_예외가_발생한다")
	void getTransactionHistory_period와날짜동시지정() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));

		assertThatThrownBy(() -> accountService.getTransactionHistory(USER_ID, "WEEK",
			LocalDate.of(2024, 1, 1), null, null, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_REQUEST);

		then(financeClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("getTransactionHistory_startDate만_지정하면_INVALID_REQUEST_예외가_발생한다")
	void getTransactionHistory_startDate만지정() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));

		assertThatThrownBy(() -> accountService.getTransactionHistory(USER_ID, null,
			LocalDate.of(2024, 1, 1), null, null, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_REQUEST);

		then(financeClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("getTransactionHistory_endDate가_startDate보다_이전이면_INVALID_REQUEST_예외가_발생한다")
	void getTransactionHistory_기간역전() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));

		assertThatThrownBy(() -> accountService.getTransactionHistory(USER_ID, null,
			LocalDate.of(2024, 3, 1), LocalDate.of(2024, 1, 1), null, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_REQUEST);

		then(financeClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("getTransactionHistory_조회기간이_3개월을_초과하면_INVALID_REQUEST_예외가_발생한다")
	void getTransactionHistory_최대기간초과() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));

		assertThatThrownBy(() -> accountService.getTransactionHistory(USER_ID, null,
			LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 1), null, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_REQUEST);

		then(financeClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("getTransactionHistory_유효하지_않은_period값이면_INVALID_REQUEST_예외가_발생한다")
	void getTransactionHistory_잘못된period() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));

		assertThatThrownBy(
			() -> accountService.getTransactionHistory(USER_ID, "YEAR", null, null, null, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_REQUEST);

		then(financeClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("getTransactionHistory_transactionType이_DEPOSIT이면_금융망에_M코드로_요청한다")
	void getTransactionHistory_입금필터() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));
		InquireTransactionHistoryListRec rec = InquireTransactionHistoryListRec.builder()
			.totalCount("0")
			.list(List.of())
			.build();
		given(financeClient.inquireTransactionHistoryList(eq(USER_KEY), eq(ACCOUNT_NO), anyString(), anyString(),
			eq("M"), eq("DESC"))).willReturn(rec);

		accountService.getTransactionHistory(USER_ID, null, null, null, null, null, "DEPOSIT");

		then(financeClient).should()
			.inquireTransactionHistoryList(eq(USER_KEY), eq(ACCOUNT_NO), anyString(), anyString(), eq("M"),
				eq("DESC"));
	}

	@Test
	@DisplayName("getTransactionHistory_transactionType이_WITHDRAWAL이면_금융망에_D코드로_요청한다")
	void getTransactionHistory_출금필터() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));
		InquireTransactionHistoryListRec rec = InquireTransactionHistoryListRec.builder()
			.totalCount("0")
			.list(List.of())
			.build();
		given(financeClient.inquireTransactionHistoryList(eq(USER_KEY), eq(ACCOUNT_NO), anyString(), anyString(),
			eq("D"), eq("DESC"))).willReturn(rec);

		accountService.getTransactionHistory(USER_ID, null, null, null, null, null, "WITHDRAWAL");

		then(financeClient).should()
			.inquireTransactionHistoryList(eq(USER_KEY), eq(ACCOUNT_NO), anyString(), anyString(), eq("D"),
				eq("DESC"));
	}

	@Test
	@DisplayName("getTransactionHistory_유효하지_않은_transactionType값이면_INVALID_REQUEST_예외가_발생한다")
	void getTransactionHistory_잘못된transactionType() {
		given(accountRepository.findFirstByUserId(USER_ID)).willReturn(Optional.of(account(1L)));

		assertThatThrownBy(() -> accountService.getTransactionHistory(USER_ID, null, null, null, null, null, "ALL"))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_REQUEST);

		then(financeClient).shouldHaveNoInteractions();
	}
}
