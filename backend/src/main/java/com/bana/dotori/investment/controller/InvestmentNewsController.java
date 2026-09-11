package com.bana.dotori.investment.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.investment.dto.InvestmentNewsDetailResponse;
import com.bana.dotori.investment.dto.InvestmentNewsListResponse;
import com.bana.dotori.investment.service.InvestmentNewsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/investments/news")
@RequiredArgsConstructor
public class InvestmentNewsController {

	private final InvestmentNewsService investmentNewsService;

	@GetMapping
	public ResponseEntity<InvestmentNewsListResponse> getNews(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		@RequestParam(required = false) Integer productId) {
		return ResponseEntity.ok(investmentNewsService.getNews(date, productId));
	}

	@GetMapping("/{newsId}")
	public ResponseEntity<InvestmentNewsDetailResponse> getNewsDetail(@PathVariable Integer newsId) {
		return ResponseEntity.ok(investmentNewsService.getNewsDetail(newsId));
	}
}
