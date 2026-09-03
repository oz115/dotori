package com.bana.dotori.account.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.account.dto.AccountAuthRequest;
import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.dto.AccountTransactionHistoryResponse;
import com.bana.dotori.account.entity.Account;
import com.bana.dotori.account.mapper.AccountMapper;
import com.bana.dotori.account.repository.AccountRepository;
import com.bana.dotori.finance.client.FinanceClient;
import com.bana.dotori.finance.config.FinanceProperties;
import com.bana.dotori.finance.dto.transaction.InquireTransactionHistoryListRec;
import com.bana.dotori.finance.dto.transaction.TransactionHistoryItem;
import com.bana.dotori.finance.exception.FinanceApiException;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

	private static final int DEFAULT_TRANSACTION_PAGE_SIZE = 20;
	private static final int MAX_TRANSACTION_PAGE_SIZE = 100;
	private static final long TRANSACTION_HISTORY_LOOKBACK_DAYS = 365L;
	private static final String DEFAULT_TRANSACTION_TYPE = "A";
	private static final String ORDER_BY_DESC = "DESC";
	private static final DateTimeFormatter FINANCE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final FinanceClient financeClient;
	private final FinanceProperties financeProperties;
	private final AccountAuthSessionManager accountAuthSessionManager;
	private final AccountMapper accountMapper;

	// 금융망 userKey를 조회해 1원 송금을 요청하고 인증 세션을 생성한다
	@Override
	@Transactional(readOnly = true)
	public AccountAuthResponse requestAuth(Integer userId, AccountAuthRequest request) {
		if (accountRepository.existsByUserIdAndAccountNumber(userId, request.accountNo())) {
			throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_LINKED);
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

		String userKey;
		try {
			userKey = financeClient.getUserKey(user.getEmail());
			financeClient.openAccountAuth(userKey, request.accountNo(), financeProperties.authText());
		} catch (FinanceApiException ex) {
			throw new BusinessException(ErrorCode.FINANCE_API_ERROR);
		}

		accountAuthSessionManager.create(userId, request.accountNo(), userKey);
		return accountMapper.toAccountAuthResponse(request.accountNo(), financeProperties.authText(),
			accountAuthSessionManager.sessionTtlSeconds());
	}

	// 인증 세션의 userKey로 인증코드를 검증하고 계좌를 저장한다
	@Override
	@Transactional
	public AccountResponse verifyAuth(Integer userId, AccountAuthVerifyRequest request) {
		if (accountRepository.existsByUserIdAndAccountNumber(userId, request.accountNo())) {
			throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_LINKED);
		}
		String userKey = accountAuthSessionManager.getUserKey(userId, request.accountNo());

		try {
			financeClient.checkAuthCode(userKey, request.accountNo(), financeProperties.authText(), request.authCode());
		} catch (FinanceApiException ex) {
			accountAuthSessionManager.incrementAttempt(userId, request.accountNo());
			throw new BusinessException(ErrorCode.AUTH_CODE_MISMATCH);
		}

		accountAuthSessionManager.delete(userId, request.accountNo());
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

		Account saved = accountRepository.save(accountMapper.toAccount(user, request, userKey));
		return accountMapper.toAccountResponse(saved);
	}

	// 연동 계좌의 거래내역을 금융망에서 실시간으로 조회해 커서 기반으로 잘라 반환한다
	@Override
	@Transactional(readOnly = true)
	public AccountTransactionHistoryResponse getTransactionHistory(Integer userId, Long accountId, String cursor,
		Integer size, String transactionType) {
		Account account = accountRepository.findByIdAndUserId(accountId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

		int pageSize = (size == null) ? DEFAULT_TRANSACTION_PAGE_SIZE : Math.min(size, MAX_TRANSACTION_PAGE_SIZE);
		LocalDate endDate = LocalDate.now();
		LocalDate startDate = endDate.minusDays(TRANSACTION_HISTORY_LOOKBACK_DAYS);

		InquireTransactionHistoryListRec rec;
		try {
			rec = financeClient.inquireTransactionHistoryList(account.getFinanceUserKey(),
				account.getAccountNumber(), startDate.format(FINANCE_DATE_FORMATTER),
				endDate.format(FINANCE_DATE_FORMATTER),
				transactionType != null ? transactionType : DEFAULT_TRANSACTION_TYPE, ORDER_BY_DESC);
		} catch (FinanceApiException ex) {
			throw new BusinessException(ErrorCode.FINANCE_API_ERROR);
		}

		List<TransactionHistoryItem> items = rec.list();
		int startIndex = 0;
		if (cursor != null) {
			int cursorIndex = indexOfTransaction(items, cursor);
			startIndex = (cursorIndex >= 0) ? cursorIndex + 1 : items.size();
		}

		int endIndex = Math.min(startIndex + pageSize, items.size());
		List<TransactionHistoryItem> page = items.subList(startIndex, endIndex);
		boolean hasNext = endIndex < items.size();
		String nextCursor = hasNext ? page.get(page.size() - 1).transactionUniqueNo() : null;

		return accountMapper.toAccountTransactionHistoryResponse(page, nextCursor, hasNext);
	}

	private int indexOfTransaction(List<TransactionHistoryItem> items, String transactionUniqueNo) {
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).transactionUniqueNo().equals(transactionUniqueNo)) {
				return i;
			}
		}
		return -1;
	}
}
