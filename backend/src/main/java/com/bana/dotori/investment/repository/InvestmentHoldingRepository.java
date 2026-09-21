package com.bana.dotori.investment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.investment.entity.InvestmentHolding;

import jakarta.persistence.LockModeType;

public interface InvestmentHoldingRepository extends JpaRepository<InvestmentHolding, Integer> {

	List<InvestmentHolding> findAllByFamilyIdAndChildIdOrderByInvestmentProductIdAsc(Integer familyId,
		Integer childId);

	/** 매도 원가를 읽는 동안 재매수·동시 매도가 보유 행을 바꾸지 못하게 한다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select h from InvestmentHolding h where h.familyId = :familyId and h.childId = :childId "
		+ "and h.investmentProductId = :productId")
	Optional<InvestmentHolding> findForSell(@Param("familyId") Integer familyId,
		@Param("childId") Integer childId, @Param("productId") Integer productId);

	/** 매수 전 "이미 보유 중" 검사 — 유니크 인덱스(child_id, product_id)와 같은 기준. */
	boolean existsByChildIdAndInvestmentProductId(Integer childId, Integer investmentProductId);

	/**
	 * 매도 = 보유 행 삭제. 지운 행 수를 반환해 동시 매도(연타)를 막는다 —
	 * 두 요청이 같은 행을 지우려 하면 나중 트랜잭션은 0을 받아 지급 없이 실패한다.
	 */
	@Modifying
	@Query("delete from InvestmentHolding h where h.familyId = :familyId and h.childId = :childId "
		+ "and h.investmentProductId = :productId")
	int deleteHolding(@Param("familyId") Integer familyId, @Param("childId") Integer childId,
		@Param("productId") Integer productId);
}
