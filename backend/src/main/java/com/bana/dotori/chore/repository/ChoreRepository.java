package com.bana.dotori.chore.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.chore.entity.Chore;

public interface ChoreRepository extends JpaRepository<Chore, Integer> {

	/**
	 * 한 아이에게 배정된 활성 집안일 (CHORE-01).
	 *
	 * <p>경로가 가족과 아이를 함께 지목하므로 조회도 둘 다로 좁힌다. childId 만으로 찾으면
	 * 다른 가족의 집안일까지 나온다. 아이가 두 가족에 속하는 경우는 없지만,
	 * 조회 조건에서 가족 경계를 빼면 나중에 그런 상황이 생겼을 때 조용히 새어 나간다.
	 *
	 * <p>방금 등록한 것이 위에 오도록 최신순이고, created_at 이 같은 행은 ID 로 순서를 고정한다.
	 */
	List<Chore> findAllByFamilyIdAndChildIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(
			Integer familyId, Integer childId);

	/**
	 * 상세 조회·수정·삭제의 진입점 (CHORE-02·05·06).
	 *
	 * <p>ID 로만 찾고 나중에 가족을 확인하는 대신 처음부터 가족으로 좁힌다.
	 * 그래야 남의 가족 것을 집었을 때 403 이 아니라 404 가 나가고,
	 * "그 ID 는 존재한다"는 사실이 새지 않는다.
	 *
	 * <p>isActive 는 보지 않는다. 비활성 집안일도 상세는 조회돼야 하고(이미 지급한 보상의 근거),
	 * 걸려 있는 완료 요청을 부모가 처리할 수 있어야 한다.
	 */
	Optional<Chore> findByIdAndFamilyId(Integer id, Integer familyId);
}
