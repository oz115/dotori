package com.bana.dotori.chore.service;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;

public interface ChoreService {

	ChoreDetailResponse createChore(Integer userId, ChoreCreateRequest request);
}
