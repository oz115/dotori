package com.bana.dotori.investment.dto;

import java.time.LocalDate;
import java.util.List;

/** 가격 반영이 확인된 어제 뉴스만 회사·해설을 공개한다. 대기 항목은 정답 필드가 null이다. */
public record InvestmentNewsReviewResponse(LocalDate newsDate, List<Review> reviews) {
	public record Review(Integer investmentNewsId, String title, String content, boolean ready,
		String productName, Integer previousPrice, Integer currentPrice, String explanation) {
	}
}
