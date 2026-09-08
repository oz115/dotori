package com.bana.dotori.chore.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.entity.Chore;

@Component
public class ChoreMapper {

	public ChoreDetailResponse toDetailResponse(Chore chore) {
		return new ChoreDetailResponse(chore.getId(), chore.getFamilyId(), chore.getParentId(), chore.getChildId(),
			chore.getTitle(), chore.getDescription(), chore.getRewardPoints(), chore.getRepeatType(),
			chore.getStartDate(), chore.getEndDate(), chore.isActive(), chore.getCreatedAt());
	}
}
