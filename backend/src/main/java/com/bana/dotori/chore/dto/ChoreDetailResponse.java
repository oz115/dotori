package com.bana.dotori.chore.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.bana.dotori.chore.entity.ChoreRepeatType;

/**
 * 집안일 상세 (CHORE-02·04·05).
 *
 * <p>rewardPoints 는 "승인 시점에 지급할 금액"이다. 대기 중인 완료 요청이 있는 상태에서
 * 부모가 금액을 바꾸면 바뀐 값이 지급된다 (요청 시점 값을 스냅샷하지 않는다).
 */
public record ChoreDetailResponse(
	Integer choreId,
	Integer familyId,
	Integer parentId,
	Integer childId,
	String title,
	String description,
	Integer rewardPoints,
	ChoreRepeatType repeatType,
	LocalDate startDate,
	LocalDate endDate,
	boolean isActive,
	OffsetDateTime createdAt
) {
}
