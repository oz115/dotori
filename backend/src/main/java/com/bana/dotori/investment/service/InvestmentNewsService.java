package com.bana.dotori.investment.service;

import java.time.LocalDate;

import com.bana.dotori.investment.dto.InvestmentNewsDetailResponse;
import com.bana.dotori.investment.dto.InvestmentNewsListResponse;

public interface InvestmentNewsService {

	/**
	 * NEWS-01 투자 뉴스 목록 조회. date(기본: 오늘, 한국 시간 기준)에 발행된 뉴스를
	 * 최신순으로 준다. productId 를 주면 그 종목과 연결된 뉴스만 남긴다.
	 */
	InvestmentNewsListResponse getNews(LocalDate date, Integer productId);

	/** 투자 뉴스 상세(본문 포함) 조회. 없으면 NEWS_NOT_FOUND. */
	InvestmentNewsDetailResponse getNewsDetail(Integer newsId);
}
