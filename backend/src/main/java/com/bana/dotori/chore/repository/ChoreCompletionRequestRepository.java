package com.bana.dotori.chore.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.chore.entity.ChoreCompletionRequest;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;

public interface ChoreCompletionRequestRepository extends JpaRepository<ChoreCompletionRequest, Integer> {

	/**
	 * 이번 주기에 이미 승인된 이력이 있는지 본다 (CHORE-09 사전 검사).
	 *
	 * <p>상한을 두지 않고 주기 시작 시각 이후만 본다. 크론이 주기가 끝날 때마다 기간을 앞으로
	 * 밀어 주므로, 지금 기간의 시작일보다 뒤에 처리된 승인은 이번 주기의 것뿐이다.
	 * 상한까지 걸면 종료일이 없는 할 일에서 경계를 정할 수 없는데, 그 경우를 위해 분기를
	 * 만드는 것보다 하한만 보는 편이 짧고 틀릴 여지가 적다.
	 *
	 * <p>경계 시각은 {@link com.bana.dotori.chore.entity.Chore#currentPeriodStartedAt()} 가
	 * KST 로 환산해 넘겨준다. 여기서 다시 환산하지 않는 이유는 리포지토리가 타임존을 알면
	 * 같은 환산이 두 군데 생기기 때문이다.
	 *
	 * <p>이 검사만으로는 이중 지급을 못 막는다. 검사와 지급 사이에 틈이 있어서, 부모가 두
	 * 기기에서 동시에 승인하면 둘 다 통과한다. 최종 방어는 지급 멱등키다
	 * ({@link com.bana.dotori.chore.entity.Chore#rewardIdempotencyKey()}).
	 * 여기서 걸러 내는 것은 사용자에게 이유를 알려 주기 위해서다.
	 */
	default boolean hasApprovedSince(Integer choreId, OffsetDateTime periodStartedAt) {
		return existsByChoreIdAndStatusAndProcessedAtGreaterThanEqual(
				choreId, ChoreCompletionStatus.SUCCEED, periodStartedAt);
	}

	boolean existsByChoreIdAndStatusAndProcessedAtGreaterThanEqual(
			Integer choreId, ChoreCompletionStatus status, OffsetDateTime processedAt);

	/**
	 * 아직 부모가 처리하지 않은 요청이 걸려 있는지 본다 (CHORE-03).
	 *
	 * <p>같은 할 일에 대기 중인 요청을 여러 건 쌓지 못하게 막는다. 쌓이면 부모의 대기함에
	 * 같은 항목이 여러 줄로 뜨고, 하나를 승인한 뒤 남은 것들의 처리가 애매해진다.
	 */
	boolean existsByChoreIdAndStatus(Integer choreId, ChoreCompletionStatus status);

	/**
	 * 승인·거절할 요청을 집는다 (CHORE-09·10).
	 *
	 * <p>요청 ID 만으로 찾지 않고 할 일까지 함께 좁힌다. 그래야 남의 가족 요청을 집었을 때
	 * 404 가 나가고, 경로의 choreId 와 요청이 실제로 연결돼 있는지도 같이 확인된다.
	 */
	Optional<ChoreCompletionRequest> findByIdAndChoreId(Integer id, Integer choreId);

	/**
	 * 대기 중인 요청을 가족 단위로 모은다 (CHORE-08 부모 대기함).
	 *
	 * <p>요청에는 family_id 가 없으므로 chores 를 거쳐 좁힌다. 지워진 할 일의 요청도 포함한다 —
	 * 부모가 할 일을 지웠어도 이미 올라온 인증은 처리할 수 있어야 한다.
	 */
	@Query("select r from ChoreCompletionRequest r, Chore c "
			+ "where r.choreId = c.id and c.familyId = :familyId and r.status = :status "
			+ "order by r.requestedAt desc, r.id desc")
	List<ChoreCompletionRequest> findAllByFamilyIdAndStatus(
			@Param("familyId") Integer familyId, @Param("status") ChoreCompletionStatus status);
}
