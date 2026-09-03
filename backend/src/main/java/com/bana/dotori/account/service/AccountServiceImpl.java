package com.bana.dotori.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

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
		return accountMapper.toAccountAuthResponse(request.accountNo());
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
}
