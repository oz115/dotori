package com.bana.dotori.account.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.account.dto.AccountAuthRequest;
import com.bana.dotori.account.dto.AccountAuthResponse;
import com.bana.dotori.account.dto.AccountAuthVerifyRequest;
import com.bana.dotori.account.dto.AccountInfoResponse;
import com.bana.dotori.account.dto.AccountResponse;
import com.bana.dotori.account.dto.AccountTransactionHistoryResponse;
import com.bana.dotori.account.dto.TransferRequest;
import com.bana.dotori.account.dto.TransferResponse;
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

	// 연동 계좌 상세 정보 조회 API (금융망 실시간 조회)
	@GetMapping("/me")
	public ResponseEntity<AccountInfoResponse> getAccountInfo(@AuthenticationPrincipal Integer userId) {
		return ResponseEntity.ok(accountService.getAccountInfo(userId));
	}

	// 연동 계좌 거래내역 조회 API (커서 기반 무한 스크롤, 항상 최신순 정렬)
	// period(상대 기간: WEEK/MONTH/THREE_MONTHS) 또는 startDate·endDate(절대 기간) 중 하나만 지정한다
	// transactionType은 DEPOSIT/WITHDRAWAL 중 하나이며 생략하면 전체 조회한다
	@GetMapping("/transactions")
	public ResponseEntity<AccountTransactionHistoryResponse> getTransactionHistory(
		@AuthenticationPrincipal Integer userId, @RequestParam(required = false) String period,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
		@RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size,
		@RequestParam(required = false) String transactionType) {
		return ResponseEntity.ok(
			accountService.getTransactionHistory(userId, period, startDate, endDate, cursor, size, transactionType));
	}

	// 연동 계좌 간 이체 API
	@PostMapping("/transfer")
	public ResponseEntity<TransferResponse> transfer(@AuthenticationPrincipal Integer userId,
		@Valid @RequestBody TransferRequest request,
		@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
		return ResponseEntity.ok(accountService.transfer(userId, request, idempotencyKey));
	}

	// 계좌 연동 해제 API
	@DeleteMapping("/me")
	public ResponseEntity<Void> unlinkAccount(@AuthenticationPrincipal Integer userId) {
		accountService.unlinkAccount(userId);
		return ResponseEntity.noContent().build();
	}
}
