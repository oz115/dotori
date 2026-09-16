package com.bana.dotori.investment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.investment.dto.InvestmentProductListResponse;
import com.bana.dotori.investment.entity.InvestmentProductCategory;
import com.bana.dotori.investment.service.InvestmentProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/investments/products")
@RequiredArgsConstructor
public class InvestmentProductController {

	private final InvestmentProductService investmentProductService;

	@GetMapping
	public ResponseEntity<InvestmentProductListResponse> getProducts(@AuthenticationPrincipal Integer userId,
		@RequestParam(required = false) InvestmentProductCategory category) {
		return ResponseEntity.ok(investmentProductService.getProducts(userId, category));
	}
}
