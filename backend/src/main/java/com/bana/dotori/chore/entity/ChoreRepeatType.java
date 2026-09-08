package com.bana.dotori.chore.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 할 일 반복 주기.
 *
 * <p>DB의 chores_repeat_type enum 타입과 값이 같아야 한다.
 *
 * <p>부모는 주기만 고르고 기간은 직접 입력하지 않는다. 시작일은 등록한 날이고
 * 종료일은 주기에서 계산한다 ({@link #endDateFrom(LocalDate)}).
 */
public enum ChoreRepeatType {

	/** 그날 하루만. */
	DAY {
		@Override
		public LocalDate endDateFrom(LocalDate startDate) {
			return startDate;
		}
	},

	/**
	 * 등록한 주의 일요일까지. 화요일에 등록하면 그 주 일요일이 끝이다.
	 *
	 * <p>주가 월요일에 시작한다고 본다(ISO 8601·한국 관례). 일요일에 등록하면 그날 하루다.
	 */
	WEEK {
		@Override
		public LocalDate endDateFrom(LocalDate startDate) {
			return startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
		}
	},

	/** 등록한 달의 말일까지. 7월 8일에 등록하면 7월 31일이 끝이다. */
	MONTH {
		@Override
		public LocalDate endDateFrom(LocalDate startDate) {
			return startDate.with(TemporalAdjusters.lastDayOfMonth());
		}
	};

	/**
	 * 시작일과 이 주기로부터 종료일을 정한다.
	 *
	 * <p>종료일은 포함이다 (그날까지 수행할 수 있다).
	 */
	public abstract LocalDate endDateFrom(LocalDate startDate);
}
