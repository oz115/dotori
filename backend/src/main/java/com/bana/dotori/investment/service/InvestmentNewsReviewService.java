package com.bana.dotori.investment.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentNewsReviewResponse;
import com.bana.dotori.investment.dto.InvestmentNewsReviewResponse.Review;
import com.bana.dotori.investment.entity.InvestmentNews;
import com.bana.dotori.investment.entity.InvestmentNewsImpact;
import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.repository.InvestmentNewsProductRepository;
import com.bana.dotori.investment.repository.InvestmentNewsRepository;
import com.bana.dotori.investment.repository.InvestmentPriceHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvestmentNewsReviewService {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	// 종목 수. 노션 「회사 리스트」가 10개로 고정했다 — 시작가(용돈÷종목 수) 계산에 쓴다
	private static final int STOCK_COUNT = 10;
	private final FamilyMemberRepository familyMemberRepository;
	private final FamilyRepository familyRepository;
	private final InvestmentNewsRepository newsRepository;
	private final InvestmentNewsProductRepository newsProductRepository;
	private final InvestmentPriceHistoryRepository priceRepository;

	@Transactional(readOnly = true)
	public InvestmentNewsReviewResponse getReview(Integer userId) {
		Integer familyId = familyMemberRepository.findActiveFamilyId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		LocalDate today = LocalDate.now(KST);
		OffsetDateTime start = today.atStartOfDay(KST).toOffsetDateTime();
		List<InvestmentNews> news = newsRepository.findAllPublishedBetween(start.minusDays(1), start);
		if (news.isEmpty()) {
			return new InvestmentNewsReviewResponse(today.minusDays(1), List.of());
		}
		var mappings = newsProductRepository.findAllWithProductByNewsIds(news.stream()
			.map(InvestmentNews::getId).toList());
		List<Review> reviews = news.stream().map(item -> {
			var mapping = mappings.stream().filter(row -> row.getNews().getId().equals(item.getId())).findFirst();
			if (mapping.isEmpty()) {
				return waiting(item);
			}
			InvestmentProduct product = mapping.get().getProduct();
			var current = priceRepository
				.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
					familyId, product.getId(), start.plusDays(1));
			// 단순히 날짜가 바뀌었다고 정답을 공개하지 않는다. 이 가족의 오늘 반영 기록이 필요하다.
			if (current.isEmpty() || current.get().getRecordedAt().isBefore(start)) {
				return waiting(item);
			}
			// 이력이 없으면 가족별 시작가(주간 용돈 ÷ 종목 수 10) — 조회·주문 API 의 가격 규칙과 같다
			int previous = priceRepository
				.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
					familyId, product.getId(), start)
				.map(row -> row.getPrice())
				.orElseGet(() -> familyRepository.findByIdAndDeletedAtIsNull(familyId)
					.map(Family::getWeeklyAllowance)
					.filter(allowance -> allowance > 0)
					.map(allowance -> Math.max(1, (allowance + STOCK_COUNT / 2) / STOCK_COUNT))
					.orElse(product.getCurrentPrice()));
			int price = current.get().getPrice();
			return new Review(item.getId(), item.getTitle(), item.getContent(), true, product.getName(),
				previous, price, explanation(product, item.getImpact(), price - previous));
		}).toList();
		return new InvestmentNewsReviewResponse(today.minusDays(1), reviews);
	}

	private Review waiting(InvestmentNews news) {
		return new Review(news.getId(), news.getTitle(), news.getContent(), false, null, null, null, null);
	}

	private String explanation(InvestmentProduct product, InvestmentNewsImpact impact, int change) {
		boolean positive = impact == InvestmentNewsImpact.POSITIVE;
		String context = product.getNewsContext() == null ? "" : product.getNewsContext();
		String prefix = positive ? "회사에 유리한 변화:" : "회사에 불리한 변화:";
		String condition = context.lines().map(String::trim).filter(line -> line.startsWith(prefix))
			.map(line -> line.substring(prefix.length()).trim()).findFirst().orElse("어제의 경제 소식");
		String reason = switch (product.getName()) {
			case "시원아이스" -> positive
				? "더운 날에는 아이스크림을 찾는 사람이 늘어요."
				: "추운 날에는 아이스크림을 찾는 사람이 줄어요.";
			case "푸른들농업" -> positive
				? "병충해가 줄면 팔 수 있는 농작물이 늘어요."
				: "병충해가 늘면 팔 수 있는 농작물이 줄어요.";
			case "두근여행" -> positive
				? "쉬는 날이 길어지면 여행을 떠나기 좋아요."
				: "쉬는 날이 끝나면 여행을 떠날 시간이 줄어요.";
			case "신나는월드" -> positive
				? "날씨가 좋으면 야외 놀이공원을 찾기 좋아요."
				: "궂은 날씨에는 야외 놀이공원을 찾기 어려워요.";
			case "하늘항공" -> positive
				? "기름값이 내려가면 비행기를 운항하는 비용이 줄어요."
				: "기름값이 올라가면 비행기를 운항하는 비용이 늘어요.";
			case "번개택배" -> positive
				? "온라인 주문이 늘면 배송할 물건도 늘어요."
				: "온라인 주문이 줄면 배송할 물건도 줄어요.";
			case "튼튼건설" -> positive
				? "집을 사려는 사람이 늘면 아파트를 팔기 좋아요."
				: "집을 사려는 사람이 줄면 아파트를 팔기 어려워요.";
			case "고소한빵" -> positive
				? "밀 가격이 내려가면 빵의 재료비가 줄어요."
				: "밀 가격이 올라가면 빵의 재료비가 늘어요.";
			case "씽씽자동차" -> positive
				? "자동차를 사려는 사람이 늘면 판매에 도움이 돼요."
				: "자동차를 사려는 사람이 줄면 판매가 어려워져요.";
			case "파워배터리" -> positive
				? "원료 가격이 내려가면 배터리의 재료비가 줄어요."
				: "원료 가격이 올라가면 배터리의 재료비가 늘어요.";
			default -> positive ? "이 소식은 회사에 유리한 변화예요." : "이 소식은 회사에 불리한 변화예요.";
		};
		String result = change > 0 ? "도토리 모의투자에서는 이 영향을 반영해 가격이 올랐어요."
			: change < 0 ? "도토리 모의투자에서는 이 영향을 반영해 가격이 내렸어요."
			: "뉴스가 반영됐지만 가격은 그대로예요. 가격 하한에 닿으면 더 내려가지 않아요.";
		return condition + ". " + reason + " " + result;
	}
}
