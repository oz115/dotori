package com.bana.dotori.chore.event;

import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;

/**
 * AI 보상 산정 큐로 보내는 메시지 페이로드 (CHORE-04 등록 직후).
 *
 * <p>AI 서버는 이 정보를 근거로 value(가치 가중치)를 산정하고
 * {@code reward_points = value * families.weekly_allowance} 를 채운다.
 *
 * <p>weekly_allowance 를 여기 담지 않는 이유는 familyId 로 조회할 수 있고,
 * 담아 보내면 산정하는 사이에 부모가 용돈을 바꿨을 때 낡은 값으로 계산되기 때문이다
 * (상점 상품 메시지가 D 를 담지 않는 것과 같은 이유).
 */
public record ChoreRewardEvaluationMessage(
	Integer choreId,
	Integer familyId,
	Integer childId,
	String title,
	String description,
	ChoreDifficulty difficulty,
	ChoreRepeatType repeatType
) {
}
