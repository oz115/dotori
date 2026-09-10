package com.bana.dotori.chore.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreRepeatType;

public interface ChoreRepository extends JpaRepository<Chore, Integer> {

	/**
	 * 한 아이에게 배정된 살아 있는 집안일 (CHORE-01).
	 *
	 * <p>경로가 가족과 아이를 함께 지목하므로 조회도 둘 다로 좁힌다. childId 만으로 찾으면
	 * 다른 가족의 집안일까지 나온다. 아이가 두 가족에 속하는 경우는 없지만,
	 * 조회 조건에서 가족 경계를 빼면 나중에 그런 상황이 생겼을 때 조용히 새어 나간다.
	 *
	 * <p>방금 등록한 것이 위에 오도록 최신순이고, created_at 이 같은 행은 ID 로 순서를 고정한다.
	 */
	List<Chore> findAllByFamilyIdAndChildIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
			Integer familyId, Integer childId);

	/**
	 * 상세 조회·수정·삭제의 진입점 (CHORE-02·05·06).
	 *
	 * <p>ID 로만 찾고 나중에 가족을 확인하는 대신 처음부터 가족으로 좁힌다.
	 * 그래야 남의 가족 것을 집었을 때 403 이 아니라 404 가 나가고,
	 * "그 ID 는 존재한다"는 사실이 새지 않는다.
	 *
	 * <p>삭제 여부는 보지 않는다. 지워진 집안일도 상세는 조회돼야 하고(이미 지급한 보상의 근거),
	 * 걸려 있는 완료 요청을 부모가 처리할 수 있어야 한다.
	 */
	Optional<Chore> findByIdAndFamilyId(Integer id, Integer familyId);

	/**
	 * 주기가 끝난 반복 할 일의 기간을 다음 주기로 민다 (크론).
	 *
	 * <p>{@code end_date < today} 인 것만 건드린다. 아직 기간 안에 있는 할 일까지 밀면
	 * 부모가 방금 등록한 것의 기간이 하루 만에 바뀐다.
	 *
	 * <p>지워진 할 일은 제외한다. 되살릴 때를 대비해 기간을 굴려 둘 이유가 없다.
	 *
	 * <p>한 건씩 읽어 고치지 않고 일괄 UPDATE 로 처리한다. 가족 수가 늘면 대상이
	 * 수천 건이 되는데, 그때 건당 SELECT·UPDATE 를 돌리면 자정마다 DB 가 출렁인다.
	 * 대신 벌크 JPQL 은 영속성 컨텍스트를 거치지 않으므로 {@code @UpdateTimestamp} 가
	 * 동작하지 않는다. updated_at 을 파라미터로 직접 넣는다.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Chore c set c.startDate = :today, c.endDate = :endDate, c.updatedAt = :now "
		+ "where c.repeatType = :repeatType and c.deletedAt is null and c.endDate < :today")
	int rollPeriod(@Param("repeatType") ChoreRepeatType repeatType, @Param("today") LocalDate today,
		@Param("endDate") LocalDate endDate, @Param("now") OffsetDateTime now);
}
