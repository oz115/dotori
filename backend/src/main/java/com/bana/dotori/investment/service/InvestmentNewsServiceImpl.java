package com.bana.dotori.investment.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentNewsDetailResponse;
import com.bana.dotori.investment.dto.InvestmentNewsListResponse;
import com.bana.dotori.investment.entity.InvestmentNews;
import com.bana.dotori.investment.entity.InvestmentNewsProduct;
import com.bana.dotori.investment.mapper.InvestmentNewsMapper;
import com.bana.dotori.investment.repository.InvestmentNewsProductRepository;
import com.bana.dotori.investment.repository.InvestmentNewsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvestmentNewsServiceImpl implements InvestmentNewsService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final InvestmentNewsRepository investmentNewsRepository;
	private final InvestmentNewsProductRepository investmentNewsProductRepository;
	private final InvestmentNewsMapper investmentNewsMapper;

	@Override
	@Transactional(readOnly = true)
	public InvestmentNewsListResponse getNews(LocalDate date, Integer productId) {
		// 발행일 필터는 한국 시간 기준 하루 [00:00, 다음 날 00:00) 범위다.
		// published_at 이 TIMESTAMPTZ 라 날짜 동등 비교 대신 범위로 거른다.
		LocalDate targetDate = date != null ? date : LocalDate.now(KST);
		OffsetDateTime start = targetDate.atStartOfDay(KST).toOffsetDateTime();
		OffsetDateTime end = start.plusDays(1);

		List<InvestmentNews> newsList = productId == null
			? investmentNewsRepository.findAllPublishedBetween(start, end)
			: investmentNewsRepository.findAllPublishedBetweenByProductId(start, end, productId);
		if (newsList.isEmpty()) {
			return new InvestmentNewsListResponse(List.of());
		}

		// 뉴스별 관련 종목을 id 묶음으로 한 번에 가져와 그룹핑한다 (뉴스마다 조회하면 N+1)
		Map<Integer, List<InvestmentNewsProduct>> productsByNewsId = investmentNewsProductRepository
			.findAllWithProductByNewsIds(newsList.stream().map(InvestmentNews::getId).toList())
			.stream()
			.collect(Collectors.groupingBy(mapping -> mapping.getNews().getId()));

		return new InvestmentNewsListResponse(newsList.stream()
			.map(news -> investmentNewsMapper.toSummary(news,
				productsByNewsId.getOrDefault(news.getId(), List.of())))
			.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public InvestmentNewsDetailResponse getNewsDetail(Integer newsId) {
		InvestmentNews news = investmentNewsRepository.findById(newsId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NEWS_NOT_FOUND));
		return investmentNewsMapper.toDetailResponse(news,
			investmentNewsProductRepository.findAllWithProductByNewsIds(List.of(newsId)));
	}
}
