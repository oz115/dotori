package com.bana.dotori.savings.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.savings.entity.ContributionStyle;
import com.bana.dotori.savings.entity.SavingsSubscription;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;

public interface SavingsSubscriptionRepository extends JpaRepository<SavingsSubscription, Integer> {

	List<SavingsSubscription> findByFamilyIdAndChildIdOrderByStartedAtDesc(Integer familyId, Integer childId);

	/**
	 * 부모 홈 요약이 집계할 가입 건. 끝난 건(COMPLETED·CANCELLED)은 지갑으로 빠져나가
	 * 예적금 잔액이 아니므로, 상태를 넘겨 진행 중·수령 대기 건만 가져온다.
	 */
	List<SavingsSubscription> findByFamilyIdAndChildIdAndStatusIn(Integer familyId, Integer childId,
		Collection<SavingsSubscriptionStatus> statuses);

	Optional<SavingsSubscription> findByIdAndFamilyIdAndChildId(Integer id, Integer familyId, Integer childId);

	/**
	 * 가입 멱등키로 이미 만들어진 가입을 찾는다. 재시도로 같은 키가 다시 들어왔을 때
	 * 새 가입을 만드는 대신 이걸 그대로 돌려준다.
	 */
	Optional<SavingsSubscription> findByChildIdAndIdempotencyKey(Integer childId, String idempotencyKey);

	/**
	 * 납입·해지·수령처럼 잔액을 바꾸는 경로가 잡는 행 잠금.
	 *
	 * <p>같은 가입 건에 동시에 두 요청이 들어오면 둘 다 "아직 회차가 남았다"로 읽고 둘 다 납입해
	 * 약속한 회차를 넘겨 버린다. 지갑 락은 이걸 막지 못한다 — 지갑은 포인트가 두 번 빠지는 것만 막고,
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

	/**
	 * 정산일이 곧 돌아오는 정액 적립식 가입 건. 납입 리마인더가 쓴다.
	 *
	 * <p>상품은 연관관계가 아니라 id 로만 이어져 있어 세타 조인으로 묶는다.
	 *
	 * <p>적립식만 본다 - 보통예금(수시입출금)과 거치식(가입 시 1회)은 이번 주에 넣어야 할
	 * 금액이라는 것이 없다.
	 */
	@Query("select s from SavingsSubscription s, SavingsProduct p "
		+ "where p.id = s.savingsProductId "
		+ "and s.status = :status "
		+ "and p.contributionStyle = :style "
		+ "and s.nextSettlementAt is not null "
		+ "and s.nextSettlementAt <= :deadline")
	List<SavingsSubscription> findInstallmentsSettlingBy(@Param("status") SavingsSubscriptionStatus status,
		@Param("style") ContributionStyle style, @Param("deadline") OffsetDateTime deadline);
}
