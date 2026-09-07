package com.bana.dotori.account.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.account.dto.AccountAuthRequest;
import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountInfoResponse;
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.dto.AccountTransactionHistoryResponse;
import com.bana.dotori.account.dto.TransactionDirection;
import com.bana.dotori.account.dto.TransactionPeriod;
import com.bana.dotori.account.dto.TransferRequest;
import com.bana.dotori.account.dto.TransferResponse;
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
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

	private static final int DEFAULT_TRANSACTION_PAGE_SIZE = 20;
	private static final int MAX_TRANSACTION_PAGE_SIZE = 100;
	private static final long MAX_TRANSACTION_RANGE_MONTHS = 3L;
	private static final TransactionPeriod DEFAULT_TRANSACTION_PERIOD = TransactionPeriod.MONTH;
	private static final String FINANCE_TRANSACTION_TYPE_ALL = "A";
	private static final String FINANCE_TRANSACTION_TYPE_DEPOSIT = "M";
	private static final String FINANCE_TRANSACTION_TYPE_WITHDRAWAL = "D";
	private static final String ORDER_BY_DESC = "DESC";
	private static final DateTimeFormatter FINANCE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final String DEFAULT_TRANSFER_SUMMARY = "계좌 이체";
	private static final String DEFAULT_DEPOSIT_SUMMARY = "계좌 이체 입금";

	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final FinanceClient financeClient;
	private final FinanceProperties financeProperties;
	private final AccountAuthSessionManager accountAuthSessionManager;
	private final TransferIdempotencyGuard transferIdempotencyGuard;
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

	// 연동 계좌의 상세 정보를 금융망에서 실시간으로 조회한다
	@Override
	@Transactional(readOnly = true)
	public AccountInfoResponse getAccountInfo(Integer userId) {
		Account account = accountRepository.findFirstByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED));

		InquireDemandDepositAccountRec rec;
		try {
			rec = financeClient.inquireDemandDepositAccount(account.getFinanceUserKey(), account.getAccountNumber());
		} catch (FinanceApiException ex) {
			throw new BusinessException(ErrorCode.FINANCE_API_ERROR);
		}

		return accountMapper.toAccountInfoResponse(rec);
	}

	// 연동 계좌의 거래내역을 금융망에서 실시간으로 조회해 커서 기반으로 잘라 반환한다
	@Override
	@Transactional(readOnly = true)
	public AccountTransactionHistoryResponse getTransactionHistory(Integer userId, String period,
		LocalDate startDate, LocalDate endDate, String cursor, Integer size, String transactionType) {
		Account account = accountRepository.findFirstByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED));

		DateRange range = resolveDateRange(period, startDate, endDate);
		int pageSize = (size == null) ? DEFAULT_TRANSACTION_PAGE_SIZE : Math.min(size, MAX_TRANSACTION_PAGE_SIZE);

		String financeTransactionType = resolveFinanceTransactionType(transactionType);

		InquireTransactionHistoryListRec rec;
		try {
			rec = financeClient.inquireTransactionHistoryList(account.getFinanceUserKey(),
				account.getAccountNumber(), range.startDate().format(FINANCE_DATE_FORMATTER),
				range.endDate().format(FINANCE_DATE_FORMATTER), financeTransactionType, ORDER_BY_DESC);
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

	// 이체를 수행한다 (송금 계좌는 호출자 본인의 연동 계좌, 수신 계좌는 연동 여부와 무관하게 계좌번호로 지정)
	@Override
	@Transactional(readOnly = true)
	public TransferResponse transfer(Integer userId, TransferRequest request, String idempotencyKey) {
		Account fromAccount = accountRepository.findFirstByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED));

		if (fromAccount.getAccountNumber().equals(request.toAccountNo())) {
			throw new BusinessException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED);
		}

		transferIdempotencyGuard.reserve(fromAccount.getId(), idempotencyKey);

		InquireDemandDepositAccountRec fromAccountInfo;
		try {
			fromAccountInfo = financeClient.inquireDemandDepositAccount(fromAccount.getFinanceUserKey(),
				fromAccount.getAccountNumber());
		} catch (FinanceApiException ex) {
			throw new BusinessException(ErrorCode.FINANCE_API_ERROR);
		}

		long oneTimeTransferLimit = Long.parseLong(fromAccountInfo.oneTimeTransferLimit());
		long accountBalance = Long.parseLong(fromAccountInfo.accountBalance());
		if (request.amount() > oneTimeTransferLimit) {
			throw new BusinessException(ErrorCode.TRANSFER_LIMIT_EXCEEDED);
		}
		if (request.amount() > accountBalance) {
			throw new BusinessException(ErrorCode.INSUFFICIENT_ACCOUNT_BALANCE);
		}

		String summary = (request.memo() != null && !request.memo().isBlank())
			? request.memo()
			: DEFAULT_TRANSFER_SUMMARY;
		try {
			financeClient.updateDemandDepositAccountTransfer(fromAccount.getFinanceUserKey(),
				fromAccount.getAccountNumber(), request.toAccountNo(), request.amount(),
				summary, DEFAULT_DEPOSIT_SUMMARY);
		} catch (FinanceApiException ex) {
			throw new BusinessException(ErrorCode.BANK_TRANSFER_FAILED);
		}

		return accountMapper.toTransferResponse(fromAccount, request.toAccountNo(), request.amount(),
			OffsetDateTime.now());
	}

	// 연동 계좌번호만 조회한다 (금융망 호출 없이 로컬 DB만 본다)
	@Override
	@Transactional(readOnly = true)
	public String getLinkedAccountNumber(Integer userId) {
		return accountRepository.findFirstByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED))
			.getAccountNumber();
	}

	private int indexOfTransaction(List<TransactionHistoryItem> items, String transactionUniqueNo) {
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).transactionUniqueNo().equals(transactionUniqueNo)) {
				return i;
			}
		}
		return -1;
	}

	// period(상대 기간)와 startDate/endDate(절대 기간)는 배타적이며, 조회 기간은 최대 3개월로 제한한다
	private DateRange resolveDateRange(String period, LocalDate startDate, LocalDate endDate) {
		boolean hasPeriod = period != null;
		boolean hasStartDate = startDate != null;
		boolean hasEndDate = endDate != null;

		if (hasPeriod && (hasStartDate || hasEndDate)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST,
				"period와 startDate/endDate는 함께 지정할 수 없습니다");
		}
		if (hasStartDate != hasEndDate) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "startDate와 endDate는 함께 지정해야 합니다");
		}

		if (hasStartDate) {
			if (startDate.isAfter(endDate)) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, "endDate는 startDate 이후여야 합니다");
			}
			if (startDate.plusMonths(MAX_TRANSACTION_RANGE_MONTHS).isBefore(endDate)) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST,
					"조회 기간은 최대 " + MAX_TRANSACTION_RANGE_MONTHS + "개월까지 가능합니다");
			}
			return new DateRange(startDate, endDate);
		}

		TransactionPeriod resolvedPeriod = hasPeriod ? parsePeriod(period) : DEFAULT_TRANSACTION_PERIOD;
		LocalDate resolvedEndDate = LocalDate.now();
		return new DateRange(resolvedPeriod.toStartDate(resolvedEndDate), resolvedEndDate);
	}

	private TransactionPeriod parsePeriod(String period) {
		try {
			return TransactionPeriod.valueOf(period);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않은 period 값입니다: " + period);
		}
	}

	// 우리 쪽 방향 필터(DEPOSIT/WITHDRAWAL)를 금융망 요청 코드(M/D/A)로 변환한다
	private String resolveFinanceTransactionType(String transactionType) {
		if (transactionType == null) {
			return FINANCE_TRANSACTION_TYPE_ALL;
		}
		TransactionDirection direction;
		try {
			direction = TransactionDirection.valueOf(transactionType);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST,
				"유효하지 않은 transactionType 값입니다: " + transactionType);
		}
		return direction == TransactionDirection.DEPOSIT
			? FINANCE_TRANSACTION_TYPE_DEPOSIT
			: FINANCE_TRANSACTION_TYPE_WITHDRAWAL;
	}

	private record DateRange(LocalDate startDate, LocalDate endDate) {
	}
}
