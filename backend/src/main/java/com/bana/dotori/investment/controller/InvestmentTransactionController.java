package com.bana.dotori.investment.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.investment.dto.InvestmentTransactionListResponse;
import com.bana.dotori.investment.entity.InvestmentTransactionType;
import com.bana.dotori.investment.service.InvestmentProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/investments/transactions")
@RequiredArgsConstructor
public class InvestmentTransactionController {

	private final InvestmentProductService investmentProductService;

	/** childId 없으면 본인 거래 이력, 있으면 내 가족 자녀의 이력 (부모용). */
	@GetMapping
	public ResponseEntity<InvestmentTransactionListResponse> getTransactions(
		@AuthenticationPrincipal Integer userId,
		@RequestParam(required = false) Integer childId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
		@RequestParam(required = false) InvestmentTransactionType type) {
		return ResponseEntity.ok(
			investmentProductService.getTransactions(userId, childId, startDate, endDate, type));
	}
}
