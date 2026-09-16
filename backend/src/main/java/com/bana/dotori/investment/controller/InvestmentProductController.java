package com.bana.dotori.investment.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.investment.dto.InvestmentPriceHistoryResponse;
import com.bana.dotori.investment.dto.InvestmentProductDetailResponse;
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

	@GetMapping("/{productId}")
	public ResponseEntity<InvestmentProductDetailResponse> getProductDetail(
		@AuthenticationPrincipal Integer userId, @PathVariable Integer productId) {
		return ResponseEntity.ok(investmentProductService.getProductDetail(userId, productId));
	}

	@GetMapping("/{productId}/price-history")
	public ResponseEntity<InvestmentPriceHistoryResponse> getPriceHistory(
		@AuthenticationPrincipal Integer userId, @PathVariable Integer productId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		return ResponseEntity.ok(investmentProductService.getPriceHistory(userId, productId, startDate, endDate));
	}
}
