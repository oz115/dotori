package com.bana.dotori.savings.entity;

/**
 * 예적금 이자·원금이 실제로 지급되는 시점.
 */
public enum PayoutTiming {

	/** 보통예금 — 매주 정산일에 이자를 즉시 지급 */
	WEEKLY_IMMEDIATE,

	/** 단리·복리·소액·중기·장기적금 — 만기에 원금과 이자를 한 번에 지급 */
	MATURITY_LUMP_SUM
}
