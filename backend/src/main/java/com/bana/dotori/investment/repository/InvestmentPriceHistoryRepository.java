package com.bana.dotori.investment.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.investment.entity.InvestmentPriceHistory;

public interface InvestmentPriceHistoryRepository extends JpaRepository<InvestmentPriceHistory, Integer> {

	/**
	 * 한 상품의 최신 가격 행 (상세 조회용). 목록용 findLatestPerProductBefore 와 같은
	 * 기준시각 규칙 — 내일 자정이면 현재가, 오늘 자정이면 전일가.
	 */
	Optional<InvestmentPriceHistory> findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
		Integer familyId, Integer investmentProductId, OffsetDateTime before);

	/**
	 * 가족의 상품별 최신 가격 행 — 기록 시각이 before 미만인 것 중 가장 최근 것 하나씩.
	 * before 에 먼 미래를 주면 현재가, 오늘(KST) 자정을 주면 전일가가 된다.
	 * (family_id, investment_product_id, recorded_at) 인덱스를 탄다.
	 */
	@Query("select ph from InvestmentPriceHistory ph "
		+ "where ph.familyId = :familyId and ph.recordedAt < :before "
		+ "and ph.recordedAt = (select max(ph2.recordedAt) from InvestmentPriceHistory ph2 "
		+ "where ph2.familyId = ph.familyId "
		+ "and ph2.investmentProductId = ph.investmentProductId and ph2.recordedAt < :before)")
	List<InvestmentPriceHistory> findLatestPerProductBefore(@Param("familyId") Integer familyId,
		@Param("before") OffsetDateTime before);

	/**
	 * 한 상품의 가장 오래된 가격 행 — 차트 시작일 하한(이 가족의 기록이 시작된 날)에 쓴다.
	 * 온보딩 백필이 지난 일주일치를 만들어 주므로, 가입일이 아니라 이 값이 차트의 왼쪽 끝이다.
	 */
	Optional<InvestmentPriceHistory> findFirstByFamilyIdAndInvestmentProductIdOrderByRecordedAtAscIdAsc(
		Integer familyId, Integer investmentProductId);

	/**
	 * 한 상품의 기간 내 가격 이력, 시간 오름차순 (INV-T-06 차트 데이터).
	 * 날짜 필터는 서비스가 한국 시간 기준 [시작일 00:00, 종료일 다음날 00:00) 범위로 바꿔 넘긴다.
	 */
	@Query("select ph from InvestmentPriceHistory ph "
		+ "where ph.familyId = :familyId and ph.investmentProductId = :productId "
		+ "and ph.recordedAt >= :start and ph.recordedAt < :end "
		+ "order by ph.recordedAt asc, ph.id asc")
	List<InvestmentPriceHistory> findAllInRange(@Param("familyId") Integer familyId,
		@Param("productId") Integer productId, @Param("start") OffsetDateTime start,
		@Param("end") OffsetDateTime end);
}
