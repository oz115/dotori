package com.bana.dotori.chore.entity;

/**
 * 집안일 반복 주기.
 *
 * <p>DB의 chores_repeat_type enum 타입과 값이 같아야 한다.
 * 지금은 "얼마나 자주 하는 일인지"를 화면에 보여 주는 용도이고,
 * 주기당 완료 횟수를 제한하지는 않는다 (한 집안일에 PENDING 요청 1건만 허용하는 방식).
 */
public enum ChoreRepeatType {
	DAY,
	WEEK,
	MONTH
}
