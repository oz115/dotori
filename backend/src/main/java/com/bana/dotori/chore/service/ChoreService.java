package com.bana.dotori.chore.service;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreListResponse;

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
}
