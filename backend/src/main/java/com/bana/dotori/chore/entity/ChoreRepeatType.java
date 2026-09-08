package com.bana.dotori.chore.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 할 일 반복 주기.
 *
 * <p>DB의 chores_repeat_type enum 타입과 값이 같아야 한다.
 *
 * <p>{@link #ONCE} 는 반복하지 않는 일회성이다. 화면의 "반복" 체크를 끄면 이 값이 되고,
 * 그때는 부모가 시작일과 종료일을 직접 넣는다.
 *
 * <p>나머지 셋은 반복이다. 부모는 주기만 고르고 기간은 등록한 날에서 계산한다
 * ({@link #endDateFrom(LocalDate)}). 주기가 끝나면 크론이 다음 주기로 기간을 밀어 준다.
 */
public enum ChoreRepeatType {

	/**
	 * 반복하지 않는 일회성. 기간을 계산하지 않고 부모가 넣은 값을 그대로 쓴다.
	 *
	 * <p>{@link #endDateFrom(LocalDate)} 을 호출하면 시작일을 그대로 돌려준다.
	 * 이 값에는 애초에 그 계산을 쓸 일이 없지만, 잘못 불렸을 때 엉뚱한 날짜가
	 * 나오는 것보다 시작일과 같은 하루로 보이는 편이 덜 위험하다.
	 */
	ONCE {
		@Override
		public LocalDate endDateFrom(LocalDate startDate) {
			return startDate;
		}

		@Override
		public boolean isRepeating() {
			return false;
		}
	},

	/** 그날 하루만. 크론이 매일 자정에 다음 날로 밀어 준다. */
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
	 * 크론이 매주 월요일 자정에 그 주로 밀어 준다.
	 */
	WEEK {
		@Override
		public LocalDate endDateFrom(LocalDate startDate) {
			return startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
		}
	},

	/**
	 * 등록한 달의 말일까지. 7월 8일에 등록하면 7월 31일이 끝이다.
	 * 크론이 매월 1일 자정에 그 달로 밀어 준다.
	 */
	MONTH {
		@Override
		public LocalDate endDateFrom(LocalDate startDate) {
			return startDate.with(TemporalAdjusters.lastDayOfMonth());
		}
	};

	/**
	 * 시작일과 이 주기로부터 종료일을 정한다. 종료일은 포함이다 (그날까지 수행할 수 있다).
	 */
	public abstract LocalDate endDateFrom(LocalDate startDate);

	/**
	 * 크론이 기간을 밀어 줘야 하는 주기인지.
	 *
	 * <p>{@link #ONCE} 만 false 다. 일회성 할 일의 기간을 크론이 건드리면
	 * 부모가 정한 날짜가 조용히 바뀐다.
	 */
	public boolean isRepeating() {
		return true;
	}
}
