package com.bana.dotori.savings.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.savings.entity.SavingsSubscription;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;

public interface SavingsSubscriptionRepository extends JpaRepository<SavingsSubscription, Integer> {

	List<SavingsSubscription> findByFamilyIdAndChildIdOrderByStartedAtDesc(Integer familyId, Integer childId);

	Optional<SavingsSubscription> findByIdAndFamilyIdAndChildId(Integer id, Integer familyId, Integer childId);

	/**
	 * 가입 멱등키로 이미 만들어진 가입을 찾는다. 재시도로 같은 키가 다시 들어왔을 때
	 * 새 가입을 만드는 대신 이걸 그대로 돌려준다.
	 */
	Optional<SavingsSubscription> findByChildIdAndIdempotencyKey(Integer childId, String idempotencyKey);

	/**
	 * 납입·해지·수령처럼 잔액을 바꾸는 경로가 잡는 행 잠금.
	 *
	 * <p>같은 가입 건에 동시에 두 요청이 들어오면 둘 다 "아직 목표 미달"로 읽고 둘 다 납입해
	 * 목표를 넘겨 버린다. 지갑 락은 이걸 막지 못한다 — 지갑은 포인트가 두 번 빠지는 것만 막고,
	 * 여기서 깨지는 것은 가입 건의 한도다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from SavingsSubscription s "
		+ "where s.id = :id and s.familyId = :familyId and s.childId = :childId")
	Optional<SavingsSubscription> findByIdAndFamilyIdAndChildIdForUpdate(@Param("id") Integer id,
		@Param("familyId") Integer familyId, @Param("childId") Integer childId);

	/** 정산이 잡을 행 잠금. 정산은 경로에 familyId·childId 가 없어서 id 로만 찾는다 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from SavingsSubscription s where s.id = :id")
	Optional<SavingsSubscription> findByIdForUpdate(@Param("id") Integer id);

	/** 정산 시각이 지난 진행 중 가입 건. 주간 정산 스케줄러가 쓴다 */
	List<SavingsSubscription> findByStatusAndNextSettlementAtLessThanEqual(SavingsSubscriptionStatus status,
		OffsetDateTime threshold);
}
