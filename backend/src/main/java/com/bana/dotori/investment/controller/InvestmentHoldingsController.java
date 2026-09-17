package com.bana.dotori.investment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.investment.dto.InvestmentHoldingsResponse;
import com.bana.dotori.investment.service.InvestmentProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/investments/holdings")
@RequiredArgsConstructor
public class InvestmentHoldingsController {

	private final InvestmentProductService investmentProductService;

	/** childId 없으면 본인 보유, 있으면 내 가족 자녀의 보유 (부모용). */
	@GetMapping
	public ResponseEntity<InvestmentHoldingsResponse> getHoldings(@AuthenticationPrincipal Integer userId,
		@RequestParam(required = false) Integer childId) {
		return ResponseEntity.ok(investmentProductService.getHoldings(userId, childId));
	}
}
