package com.bana.dotori.chore.service;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreListResponse;

public interface ChoreService {

	ChoreDetailResponse createChore(Integer userId, ChoreCreateRequest request);

	/**
	 * 집안일 목록 (CHORE-01).
	 *
	 * @param childId 부모가 특정 자녀만 보려고 넘기는 필터. 없으면 가족 전체.
	 *                아이가 호출하면 이 값과 무관하게 자기 몫만 나온다.
	 */
	ChoreListResponse getChores(Integer userId, Integer childId);

	ChoreDetailResponse getChore(Integer userId, Integer choreId);
}
