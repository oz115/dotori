package com.bana.dotori.wallet.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.wallet.entity.PointCashoutRequest;
import com.bana.dotori.wallet.entity.PointCashoutRequestStatus;

public interface PointCashoutRequestRepository extends JpaRepository<PointCashoutRequest, Integer> {

	/**
	 * 승인 처리 전에 요청 행을 {@code SELECT ... FOR UPDATE} 로 잠근다.
	 *
	 * <p>같은 요청에 대한 동시 승인 시도가 이 지점에서 한 줄로 세워지므로,
	 * 두 번 송금되거나 상태가 두 번 바뀌는 것을 막을 수 있다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from PointCashoutRequest r where r.cashoutRequestId = :cashoutRequestId")
	Optional<PointCashoutRequest> findByCashoutRequestIdForUpdate(@Param("cashoutRequestId") Integer cashoutRequestId);

	List<PointCashoutRequest> findByFamilyIdAndChildIdOrderByRequestedAtDesc(Integer familyId, Integer childId);

	List<PointCashoutRequest> findByFamilyIdAndChildIdAndStatusOrderByRequestedAtDesc(
			Integer familyId, Integer childId, PointCashoutRequestStatus status);
}
