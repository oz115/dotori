package com.bana.dotori.chore.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;

/**
 * 목록용 집안일 요약 (CHORE-01).
 *
 * <p>familyId·parentId 는 담지 않는다. 목록은 언제나 내 가족 것뿐이라 값이 늘 같고,
 * 누가 등록했는지는 상세에서 보면 된다.
 */
public record ChoreSummary(
	Integer choreId,
	Integer childId,
	String title,
	ChoreDifficulty difficulty,
	Integer rewardPoints,
	ChoreRepeatType repeatType,
	LocalDate startDate,
	LocalDate endDate,
	OffsetDateTime createdAt
) {
}
