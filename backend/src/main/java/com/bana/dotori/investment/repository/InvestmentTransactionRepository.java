package com.bana.dotori.investment.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.investment.entity.InvestmentTransaction;
import com.bana.dotori.investment.entity.InvestmentTransactionType;

public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransaction, Integer> {

	/**
	 * 아이의 기간 내 거래 이력, 최신순 (INV-T-05). 날짜 필터는 서비스가
	 * 한국 시간 기준 하루 경계 범위로 바꿔 넘긴다 (created_at 이 TIMESTAMPTZ 라서).
	 */
	@Query("select t from InvestmentTransaction t "
		+ "where t.familyId = :familyId and t.childId = :childId "
		+ "and t.createdAt >= :start and t.createdAt < :end "
		+ "order by t.createdAt desc, t.id desc")
	List<InvestmentTransaction> findAllInRange(@Param("familyId") Integer familyId,
		@Param("childId") Integer childId, @Param("start") OffsetDateTime start,
		@Param("end") OffsetDateTime end);

	/** 위와 같되 거래 유형(BUY/SELL)까지 거른다. */
	@Query("select t from InvestmentTransaction t "
		+ "where t.familyId = :familyId and t.childId = :childId and t.transactionType = :type "
		+ "and t.createdAt >= :start and t.createdAt < :end "
		+ "order by t.createdAt desc, t.id desc")
	List<InvestmentTransaction> findAllInRangeByType(@Param("familyId") Integer familyId,
		@Param("childId") Integer childId, @Param("type") InvestmentTransactionType type,
		@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
