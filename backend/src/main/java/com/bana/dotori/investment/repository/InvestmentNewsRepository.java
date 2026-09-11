package com.bana.dotori.investment.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.investment.entity.InvestmentNews;

public interface InvestmentNewsRepository extends JpaRepository<InvestmentNews, Integer> {

	/**
	 * 발행 시각이 [start, end) 범위인 뉴스를 최신순으로. 날짜 필터는 서비스가
	 * 한국 시간 기준 하루 범위로 바꿔 넘긴다 (published_at 이 TIMESTAMPTZ 라서).
	 */
	@Query("select n from InvestmentNews n "
		+ "where n.publishedAt >= :start and n.publishedAt < :end "
		+ "order by n.publishedAt desc, n.id desc")
	List<InvestmentNews> findAllPublishedBetween(@Param("start") OffsetDateTime start,
		@Param("end") OffsetDateTime end);

	/** 위와 같되 특정 종목과 연결된 뉴스만 (NEWS-01 의 productId 필터). */
	@Query("select n from InvestmentNews n "
		+ "where n.publishedAt >= :start and n.publishedAt < :end "
		+ "and exists (select 1 from InvestmentNewsProduct np "
		+ "where np.news = n and np.product.id = :productId) "
		+ "order by n.publishedAt desc, n.id desc")
	List<InvestmentNews> findAllPublishedBetweenByProductId(@Param("start") OffsetDateTime start,
		@Param("end") OffsetDateTime end, @Param("productId") Integer productId);
}
