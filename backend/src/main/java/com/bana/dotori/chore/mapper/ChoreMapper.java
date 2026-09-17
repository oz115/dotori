package com.bana.dotori.chore.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreListResponse;
import com.bana.dotori.chore.dto.ChoreSummary;
import com.bana.dotori.chore.entity.Chore;

@Component
public class ChoreMapper {

	public ChoreListResponse toListResponse(List<Chore> chores) {
		return new ChoreListResponse(chores.stream()
			.map(this::toSummary)
			.toList());
	}

	public ChoreSummary toSummary(Chore chore) {
		return new ChoreSummary(chore.getId(), chore.getChildId(), chore.getTitle(), chore.getDifficulty(),
			chore.getRewardPoints(), chore.getRepeatType(), chore.getStartDate(), chore.getEndDate(),
			chore.getCreatedAt());
	}

	public ChoreDetailResponse toDetailResponse(Chore chore) {
		return new ChoreDetailResponse(chore.getId(), chore.getFamilyId(), chore.getParentId(), chore.getChildId(),
			chore.getTitle(), chore.getDescription(), chore.getDifficulty(), chore.getValue(),
			chore.getRewardPoints(), chore.getReason(), chore.getRepeatType(), chore.getStartDate(),
			chore.getEndDate(), chore.getCreatedAt(), chore.getDeletedAt());
	}
}
