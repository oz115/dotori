package com.bana.dotori.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.investment.entity.InvestmentNews;
import com.bana.dotori.investment.entity.InvestmentNewsImpact;
import com.bana.dotori.investment.entity.InvestmentNewsProduct;
import com.bana.dotori.investment.entity.InvestmentPriceHistory;
import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.repository.InvestmentNewsProductRepository;
import com.bana.dotori.investment.repository.InvestmentNewsRepository;
import com.bana.dotori.investment.repository.InvestmentPriceHistoryRepository;

@ExtendWith(MockitoExtension.class)
class InvestmentNewsReviewServiceTest {
	@Mock
	private FamilyMemberRepository familyMemberRepository;
	@Mock
	private FamilyRepository familyRepository;
	@Mock
	private InvestmentNewsRepository newsRepository;
	@Mock
	private InvestmentNewsProductRepository newsProductRepository;
	@Mock
	private InvestmentPriceHistoryRepository priceRepository;
	@InjectMocks
	private InvestmentNewsReviewService service;

	private OffsetDateTime start() {
		return LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
	}

	private void givenNews() {
		given(familyMemberRepository.findActiveFamilyId(1)).willReturn(Optional.of(7));
		InvestmentNews news = InvestmentNews.builder().title("밀 가격 상승").content("밀 수확량이 줄었어요.")
			.impact(InvestmentNewsImpact.NEGATIVE).publishedAt(start().minusHours(12)).build();
		ReflectionTestUtils.setField(news, "id", 3);
		InvestmentProduct product = InvestmentProduct.builder().name("고소한빵").currentPrice(1000).build();
		ReflectionTestUtils.setField(product, "id", 8);
		ReflectionTestUtils.setField(product, "newsContext", "회사에 불리한 변화: 밀 가격 상승");
		given(newsRepository.findAllPublishedBetween(start().minusDays(1), start())).willReturn(List.of(news));
		given(newsProductRepository.findAllWithProductByNewsIds(List.of(3)))
			.willReturn(List.of(InvestmentNewsProduct.builder().news(news).product(product).build()));
	}

	@Test
	void getReview_noFamily() {
		assertThatThrownBy(() -> service.getReview(1)).isInstanceOf(BusinessException.class);
	}

	@Test
	void getReview_noNews() {
		given(familyMemberRepository.findActiveFamilyId(1)).willReturn(Optional.of(7));
		assertThat(service.getReview(1).reviews()).isEmpty();
	}

	@Test
	void getReview_pendingHidesAnswer() {
		givenNews();
		given(priceRepository
			.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
				7, 8, start().plusDays(1)))
			.willReturn(Optional.of(InvestmentPriceHistory.builder().price(1000)
				.recordedAt(start().minusMinutes(1)).build()));
		var review = service.getReview(1).reviews().get(0);
		assertThat(review.ready()).isFalse();
		assertThat(review.productName()).isNull();
		assertThat(review.explanation()).isNull();
		assertThat(review.currentPrice()).isNull();
	}

	@Test
	void getReview_reflectedPriceAndReason() {
		givenNews();
		given(priceRepository
			.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
				7, 8, start().plusDays(1)))
			.willReturn(Optional.of(InvestmentPriceHistory.builder().price(900)
				.recordedAt(start().plusMinutes(5)).build()));
		var review = service.getReview(1).reviews().get(0);
		assertThat(review.ready()).isTrue();
		assertThat(review.productName()).isEqualTo("고소한빵");
		assertThat(review.previousPrice()).isEqualTo(1000);
		assertThat(review.currentPrice()).isEqualTo(900);
		assertThat(review.explanation()).contains("재료비가 늘어요", "내렸어요");
	}

	@Test
	void getReview_floorIsFlatNotFalling() {
		givenNews();
		given(priceRepository
			.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
				7, 8, start().plusDays(1)))
			.willReturn(Optional.of(InvestmentPriceHistory.builder().price(100)
				.recordedAt(start().plusMinutes(5)).build()));
		given(priceRepository
			.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(7, 8, start()))
			.willReturn(Optional.of(InvestmentPriceHistory.builder().price(100)
				.recordedAt(start().minusDays(1)).build()));
		var review = service.getReview(1).reviews().get(0);
		assertThat(review.explanation()).contains("그대로", "가격 하한").doesNotContain("내렸어요");
	}
}
