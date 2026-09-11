package com.bana.dotori.chore.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.chore.dto.ChoreCompletionRow;
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
	 * 상태를 바꾸기 전에 요청 행을 {@code SELECT ... FOR UPDATE} 로 잠근다 (CHORE-09·10·12).
	 *
	 * <p>승인·거절·취소가 모두 이 메서드로 행을 집는다. 잠그지 않으면 <b>갱신 손실</b>이 난다.
	 * 부모가 승인하는 동시에 아이가 취소하면 두 트랜잭션이 각자 PENDING 을 읽고 각자
	 * 상태를 바꾸는데, 엔티티의 PENDING 검사는 자기 트랜잭션이 읽은 값만 보므로 둘 다 통과한다.
	 * 그리고 나중에 커밋한 쪽이 앞의 UPDATE 를 덮어쓴다.
	 *
	 * <p>승인이 먼저 커밋되고 취소가 나중에 커밋되면 <b>포인트는 지급됐는데 상태는 CANCELLED</b>
	 * 인 행이 남는다. 지급은 지갑 멱등키가 막아 주지만 그건 "두 번 지급"만 막는 장치라
	 * 이 조합에는 걸리지 않는다.
	 *
	 * <p>여기서 잠근 뒤에는 뒤에 온 트랜잭션이 앞의 커밋 결과를 보게 되므로
	 * ({@code READ COMMITTED} 에서 락 해제 후 재조회) 엔티티의 PENDING 검사가 제대로 걸러 낸다.
	 * 지갑이 잔액을 바꾸기 전에 지갑 행을 잠그는 것과 같은 이유다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from ChoreCompletionRequest r where r.id = :id")
	Optional<ChoreCompletionRequest> findByIdForUpdate(@Param("id") Integer id);

	/**
	 * 부모 대기함 한 화면 (CHORE-08).
	 *
	 * <p>경로가 아이를 지목하므로 그 아이 것만 모은다. 가족 조건도 함께 거는 것은 방어용이다 —
	 * 접근 검증이 이미 childId 가 그 가족의 자녀임을 확인하지만, 조회 조건에서 가족 경계를
	 * 빼 두면 나중에 그 검증이 느슨해졌을 때 조용히 새어 나간다.
	 *
	 * <p>지워진 할 일의 요청도 포함한다. 부모가 할 일을 지웠어도 이미 올라온 인증은
	 * 처리할 수 있어야 한다.
	 *
	 * <p>제목·이름을 생성자 표현식으로 한 번에 채운다. 요청을 먼저 읽고 건당 다시 조회하면
	 * 한 화면에 쿼리가 수십 번 나간다. 이 프로젝트의 다른 응답은 {@code @Component} 매퍼가
	 * 만들지만, 조인 결과를 담는 이 목록만 예외로 둔다.
	 *
	 * <p>응답 타입이 아니라 {@link ChoreCompletionRow} 를 돌려준다. 사진 칸에 저장된 키가
	 * 들어 있어서, 서비스가 그 키로 presigned URL 을 발급해 응답 타입으로 바꿔야 한다.
	 */
	@Query("select new com.bana.dotori.chore.dto.ChoreCompletionRow("
			+ "r.id, r.choreId, c.title, u.name, r.photoKey, r.description, r.status, r.requestedAt) "
			+ "from ChoreCompletionRequest r, Chore c, User u "
			+ "where r.choreId = c.id and r.childId = u.id "
			+ "and c.familyId = :familyId and r.childId = :childId and r.status = :status "
			+ "order by r.requestedAt desc, r.id desc")
	List<ChoreCompletionRow> findSummaries(@Param("familyId") Integer familyId,
			@Param("childId") Integer childId, @Param("status") ChoreCompletionStatus status);
}
