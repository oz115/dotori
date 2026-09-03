package com.bana.dotori.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

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
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.entity.Account;
import com.bana.dotori.account.mapper.AccountMapper;
import com.bana.dotori.account.repository.AccountRepository;
import com.bana.dotori.finance.client.FinanceClient;
import com.bana.dotori.finance.config.FinanceProperties;
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
}
