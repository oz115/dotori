package com.bana.dotori.chore.dto;

import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 집안일 생성 요청 (CHORE-04).
 *
 * <p>부모가 넣는 것은 이 네 가지뿐이다. 화면에도 이만큼만 있다.
 *
 * <p>보상은 받지 않는다. AI 가 등록 직후 비동기로 가치 가중치(value)를 산정하고
 * 주간 기본 용돈(families.weekly_allowance)을 곱해 정한다. 부모가 직접 정하게 하면
 * 비현실적인 보상으로 포인트 체계가 무너지므로 의도적으로 막았다
 * (상점 상품의 price 와 같은 기준).
 *
 * <p>기간도 받지 않는다. 시작일은 등록한 날이고 종료일은 주기에서 계산한다 —
 * 주간이면 그 주 일요일, 월간이면 그 달 말일 ({@link ChoreRepeatType#endDateFrom}).
 *
 * <p>childId·familyId 는 경로가 이미 지목하고 있어 본문에 두지 않는다.
 * 두 군데서 받으면 값이 어긋날 때 무엇을 믿을지 정해야 한다.
 */
public record ChoreCreateRequest(
	@NotBlank @Size(max = 100) String title,
	@Size(max = 255) String description,
	@NotNull ChoreDifficulty difficulty,
	@NotNull ChoreRepeatType repeatType
) {
}
