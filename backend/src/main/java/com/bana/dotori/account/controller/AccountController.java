package com.bana.dotori.account.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.account.dto.AccountAuthRequest;
import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.dto.AccountTransactionHistoryResponse;
import com.bana.dotori.account.service.AccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	// 1원 인증 요청 API
	@PostMapping("/auth")
	public ResponseEntity<AccountAuthResponse> requestAuth(@AuthenticationPrincipal Integer userId,
		@Valid @RequestBody AccountAuthRequest request) {
		return ResponseEntity.ok(accountService.requestAuth(userId, request));
	}

	// 1원 인증코드 검증 API
	@PostMapping("/auth/verify")
	public ResponseEntity<AccountResponse> verifyAuth(@AuthenticationPrincipal Integer userId,
		@Valid @RequestBody AccountAuthVerifyRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(accountService.verifyAuth(userId, request));
	}

	// 연동 계좌 거래내역 조회 API (커서 기반 무한 스크롤)
	@GetMapping("/{accountId}/transactions")
	public ResponseEntity<AccountTransactionHistoryResponse> getTransactionHistory(
		@AuthenticationPrincipal Integer userId, @PathVariable Long accountId,
		@RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size,
		@RequestParam(required = false) String transactionType) {
		return ResponseEntity.ok(
			accountService.getTransactionHistory(userId, accountId, cursor, size, transactionType));
	}
}
