package com.bana.dotori.chore.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;

/**
 * 집안일 상세 (CHORE-02·04·05).
 *
 * <p>rewardPoints 는 "승인 시점에 지급할 금액"이다. 대기 중인 완료 요청이 있는 상태에서
 * 부모가 금액을 바꾸면 바뀐 값이 지급된다 (요청 시점 값을 스냅샷하지 않는다).
 * AI 산정이 끝나기 전에는 value·rewardPoints·reason 이 모두 null 이다 (= 보상 산정 중).
 *
 * <p>deletedAt 이 채워져 있으면 이미 지운 집안일이다. 목록에서는 빠지지만
 * 상세와 완료 요청 처리는 계속 되므로 클라이언트가 구분할 수 있어야 한다.
 */
public record ChoreDetailResponse(
	Integer choreId,
	Integer familyId,
	Integer parentId,
	Integer childId,
	String title,
	String description,
	ChoreDifficulty difficulty,
	BigDecimal value,
	Integer rewardPoints,
	String reason,
	ChoreRepeatType repeatType,
	LocalDate startDate,
	LocalDate endDate,
	OffsetDateTime createdAt,
	OffsetDateTime deletedAt
) {
}
