package com.bana.dotori.chore.service;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreListResponse;
import com.bana.dotori.chore.dto.ChoreUpdateRequest;

/**
 * 집안일 서비스 (CHORE).
 *
 * <p>모든 메서드가 familyId·childId 를 함께 받는다. 경로에서 온 값이므로 믿을 수 없고,
 * 구현체가 매번 호출자의 소속과 아이의 소속을 확인한다.
 */
public interface ChoreService {

	ChoreDetailResponse createChore(Integer userId, Integer familyId, Integer childId, ChoreCreateRequest request);

	ChoreListResponse getChores(Integer userId, Integer familyId, Integer childId);

	ChoreDetailResponse getChore(Integer userId, Integer familyId, Integer childId, Integer choreId);

	ChoreDetailResponse updateChore(Integer userId, Integer familyId, Integer childId, Integer choreId,
		ChoreUpdateRequest request);

	/**
	 * 집안일을 목록에서 감춘다 (CHORE-06).
	 *
	 * <p>물리 삭제가 아니라 isActive=false 다. 이미 비활성인 것을 다시 지워도 성공으로 본다 —
	 * 결과 상태가 같으므로 재시도가 실패로 보이면 클라이언트가 더 헷갈린다.
	 */
	void deactivateChore(Integer userId, Integer familyId, Integer childId, Integer choreId);
}
