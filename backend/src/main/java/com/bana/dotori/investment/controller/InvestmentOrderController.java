package com.bana.dotori.investment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.investment.dto.InvestmentOrderRequest;
import com.bana.dotori.investment.dto.InvestmentOrderResponse;
import com.bana.dotori.investment.service.InvestmentOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/investments/orders")
@RequiredArgsConstructor
public class InvestmentOrderController {

	private final InvestmentOrderService investmentOrderService;

	@PostMapping("/buy")
	public ResponseEntity<InvestmentOrderResponse> buy(@AuthenticationPrincipal Integer userId,
		@Valid @RequestBody InvestmentOrderRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(investmentOrderService.buy(userId, request));
	}

	@PostMapping("/sell")
	public ResponseEntity<InvestmentOrderResponse> sell(@AuthenticationPrincipal Integer userId,
		@Valid @RequestBody InvestmentOrderRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(investmentOrderService.sell(userId, request));
	}
}
