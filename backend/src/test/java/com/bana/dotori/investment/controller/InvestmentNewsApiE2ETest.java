package com.bana.dotori.investment.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.user.entity.UserRole;

/**
 * E2E - 투자 뉴스 조회가 인증부터 실제 DB 까지 끝까지 도는지 확인한다.
 *
 * <p>계층 테스트(Mockito)로는 검증할 수 없는 것들이 대상이다.
 * JPQL 이 실제로 도는지(발행 범위·최신순 정렬·productId exists 필터·관련 종목 fetch join),
 * 네이티브 ENUM(investment_news.impact)이 @Enumerated(STRING) 매핑으로 왕복하는지,
 * 그리고 UTC 로 저장된 TIMESTAMPTZ(published_at — AI 서버가 UTC now 로 넣는다)가
 * 한국 시간 하루 경계 필터에 정확히 걸리는지다. H2 로는 셋 다 흉내가 안 된다.
 *
 * <p>이 API 는 userId 를 쓰지 않아 회원 없이 실제 JwtTokenProvider 토큰만으로 호출한다.
 * 투자 테이블은 전역(가족 무관)이라 뒷정리는 [autotest] 접두사가 붙은 행만 지운다.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("투자 뉴스 API E2E")
class InvestmentNewsApiE2ETest {

	private static final String NEWS_URL = "/api/v1/investments/news";
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer productA;
	private Integer productB;

	@BeforeEach
	void setUp() {
		cleanUpInvestmentRows();
		productA = insertProduct("[autotest] 팥빙수 회사", "STOCK");
		productB = insertProduct("[autotest] 로봇 회사", null);
	}

	@AfterEach
	void tearDown() {
		cleanUpInvestmentRows();
	}

	private void cleanUpInvestmentRows() {
		jdbcTemplate.update("DELETE FROM investment_news_products WHERE investment_news_id IN "
			+ "(SELECT investment_news_id FROM investment_news WHERE title LIKE '[autotest]%')");
		jdbcTemplate.update("DELETE FROM investment_news WHERE title LIKE '[autotest]%'");
		jdbcTemplate.update("DELETE FROM investment_products WHERE name LIKE '[autotest]%'");
	}

	private Integer insertProduct(String name, String category) {
		return jdbcTemplate.queryForObject(
			"INSERT INTO investment_products (name, category, news_context, current_price, is_active) "
				+ "VALUES (?, ?::investment_products_category, '[autotest] 컨텍스트', 10000, TRUE) "
				+ "RETURNING investment_product_id",
			Integer.class, name, category);
	}

	/** AI 서버(generate_todays_news)와 같은 모양으로 뉴스 + 종목 연결(1:1)을 넣는다. */
	private Integer insertNews(String title, String impact, OffsetDateTime publishedAt, Integer productId) {
		Integer newsId = jdbcTemplate.queryForObject(
			"INSERT INTO investment_news (title, content, impact, published_at) "
				+ "VALUES (?, ?, ?::investment_news_impact, ?) RETURNING investment_news_id",
			Integer.class, title, title + " 본문", impact, publishedAt);
		if (productId != null) {
			jdbcTemplate.update("INSERT INTO investment_news_products "
				+ "(investment_news_id, investment_product_id) VALUES (?, ?)", newsId, productId);
		}
		return newsId;
	}

	private String bearer() {
		return "Bearer " + jwtTokenProvider.createAccessToken(1, UserRole.CHILD);
	}

	private OffsetDateTime todayStartKst() {
		return LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime();
	}

	@Nested
	@DisplayName("목록 조회 (NEWS-01)")
	class GetNews {

		@Test
		@DisplayName("date 없이 조회하면 오늘(KST) 뉴스만 최신순으로 나오고 어제 뉴스는 빠진다")
		void 오늘_뉴스만_최신순() throws Exception {
			OffsetDateTime dayStart = todayStartKst();
			insertNews("[autotest] 아침 뉴스", "POSITIVE", dayStart.plusHours(9), productA);
			insertNews("[autotest] 점심 뉴스", "NEGATIVE", dayStart.plusHours(12), productB);
			insertNews("[autotest] 어제 뉴스", "POSITIVE", dayStart.minusHours(1), productA);

			mockMvc.perform(get(NEWS_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.news.length()").value(2))
				.andExpect(jsonPath("$.news[0].title").value("[autotest] 점심 뉴스"))
				.andExpect(jsonPath("$.news[0].impact").value("NEGATIVE"))
				.andExpect(jsonPath("$.news[0].relatedProducts[0].productName").value("[autotest] 로봇 회사"))
				.andExpect(jsonPath("$.news[0].relatedProducts[0].category").value(nullValue()))
				.andExpect(jsonPath("$.news[1].title").value("[autotest] 아침 뉴스"))
				.andExpect(jsonPath("$.news[1].impact").value("POSITIVE"))
				.andExpect(jsonPath("$.news[1].relatedProducts[0].category").value("STOCK"))
				.andExpect(jsonPath("$.news[1].publishedAt").value(endsWith("+09:00")));
		}

		@Test
		@DisplayName("UTC로 저장된 발행 시각도 KST 하루 경계로 정확히 걸러진다 (자정 포함, 직전·다음날 제외)")
		void KST_하루_경계() throws Exception {
			OffsetDateTime dayStart = todayStartKst();
			// AI 서버는 UTC now 로 저장한다 — 같은 순간을 UTC 표기로 넣어 TIMESTAMPTZ 환산을 검증
			insertNews("[autotest] 자정 정각", "POSITIVE",
				dayStart.withOffsetSameInstant(ZoneOffset.UTC), productA);
			insertNews("[autotest] 자정 1초 전", "POSITIVE",
				dayStart.minusSeconds(1).withOffsetSameInstant(ZoneOffset.UTC), productA);
			insertNews("[autotest] 내일 자정", "POSITIVE",
				dayStart.plusDays(1).withOffsetSameInstant(ZoneOffset.UTC), productA);

			mockMvc.perform(get(NEWS_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.news.length()").value(1))
				.andExpect(jsonPath("$.news[0].title").value("[autotest] 자정 정각"));
		}

		@Test
		@DisplayName("date 파라미터로 과거 날짜의 뉴스를 조회한다")
		void 과거_날짜_조회() throws Exception {
			OffsetDateTime pastNoon = LocalDate.of(2026, 1, 15).atStartOfDay(KST)
				.toOffsetDateTime().plusHours(12);
			insertNews("[autotest] 과거 뉴스", "NEGATIVE", pastNoon, productA);
			insertNews("[autotest] 오늘 뉴스", "POSITIVE", todayStartKst().plusHours(9), productA);

			mockMvc.perform(get(NEWS_URL)
					.header(HttpHeaders.AUTHORIZATION, bearer())
					.param("date", "2026-01-15"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.news.length()").value(1))
				.andExpect(jsonPath("$.news[0].title").value("[autotest] 과거 뉴스"));
		}

		@Test
		@DisplayName("productId 를 주면 그 종목과 연결된 뉴스만 나온다")
		void 종목_필터() throws Exception {
			OffsetDateTime dayStart = todayStartKst();
			insertNews("[autotest] A사 뉴스", "POSITIVE", dayStart.plusHours(9), productA);
			insertNews("[autotest] B사 뉴스", "POSITIVE", dayStart.plusHours(10), productB);

			mockMvc.perform(get(NEWS_URL)
					.header(HttpHeaders.AUTHORIZATION, bearer())
					.param("productId", String.valueOf(productA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.news.length()").value(1))
				.andExpect(jsonPath("$.news[0].title").value("[autotest] A사 뉴스"))
				.andExpect(jsonPath("$.news[0].relatedProducts[0].investmentProductId").value(productA));
		}

		@Test
		@DisplayName("토큰 없이 호출하면 401 이다")
		void 미인증은_401() throws Exception {
			mockMvc.perform(get(NEWS_URL))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		}
	}

	@Nested
	@DisplayName("상세 조회")
	class GetNewsDetail {

		@Test
		@DisplayName("본문·영향 방향·관련 종목·한국 시간 발행 일시를 내려 준다")
		void 상세_조회() throws Exception {
			Integer newsId = insertNews("[autotest] 상세 뉴스", "NEGATIVE",
				todayStartKst().plusHours(9).withOffsetSameInstant(ZoneOffset.UTC), productA);

			mockMvc.perform(get(NEWS_URL + "/" + newsId).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.investmentNewsId").value(newsId))
				.andExpect(jsonPath("$.content").value("[autotest] 상세 뉴스 본문"))
				.andExpect(jsonPath("$.impact").value("NEGATIVE"))
				.andExpect(jsonPath("$.relatedProducts[0].productName").value("[autotest] 팥빙수 회사"))
				.andExpect(jsonPath("$.publishedAt").value(endsWith("+09:00")));
		}

		@Test
		@DisplayName("없는 뉴스는 404 NEWS_NOT_FOUND 다")
		void 없는_뉴스_404() throws Exception {
			mockMvc.perform(get(NEWS_URL + "/999999999").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NEWS_NOT_FOUND"));
		}
	}
}
