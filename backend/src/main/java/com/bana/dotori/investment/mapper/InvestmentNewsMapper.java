package com.bana.dotori.investment.mapper;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.investment.dto.InvestmentNewsDetailResponse;
import com.bana.dotori.investment.dto.InvestmentNewsSummary;
import com.bana.dotori.investment.dto.RelatedProductResponse;
import com.bana.dotori.investment.entity.InvestmentNews;
import com.bana.dotori.investment.entity.InvestmentNewsProduct;

@Component
public class InvestmentNewsMapper {

	// 명세의 발행 일시 예시가 +09:00 — DB 의 TIMESTAMPTZ 를 한국 시간으로 바꿔 내려 준다
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public InvestmentNewsSummary toSummary(InvestmentNews news, List<InvestmentNewsProduct> mappings) {
		return new InvestmentNewsSummary(news.getId(), news.getTitle(), news.getImpact(),
			toRelatedProducts(mappings), toKst(news.getPublishedAt()));
	}

	public InvestmentNewsDetailResponse toDetailResponse(InvestmentNews news,
		List<InvestmentNewsProduct> mappings) {
		return new InvestmentNewsDetailResponse(news.getId(), news.getTitle(), news.getContent(),
			news.getImpact(), toRelatedProducts(mappings), toKst(news.getPublishedAt()));
	}

	private List<RelatedProductResponse> toRelatedProducts(List<InvestmentNewsProduct> mappings) {
		return mappings.stream()
			.map(mapping -> new RelatedProductResponse(mapping.getProduct().getId(),
				mapping.getProduct().getName(), mapping.getProduct().getCategory()))
			.toList();
	}

	private OffsetDateTime toKst(OffsetDateTime value) {
		return value.atZoneSameInstant(KST).toOffsetDateTime();
	}
}
