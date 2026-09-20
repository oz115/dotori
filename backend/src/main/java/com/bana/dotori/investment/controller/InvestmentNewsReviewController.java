package com.bana.dotori.investment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.investment.dto.InvestmentNewsReviewResponse;
import com.bana.dotori.investment.service.InvestmentNewsReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/investments/news/review")
@RequiredArgsConstructor
public class InvestmentNewsReviewController {
	private final InvestmentNewsReviewService reviewService;

	@GetMapping
	public ResponseEntity<InvestmentNewsReviewResponse> getReview(@AuthenticationPrincipal Integer userId) {
		return ResponseEntity.ok(reviewService.getReview(userId));
	}
}
