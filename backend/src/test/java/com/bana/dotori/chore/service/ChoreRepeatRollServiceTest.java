package com.bana.dotori.chore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.repository.ChoreRepository;

/**
 * 2계층 - 크론이 반복 할 일의 기간을 어느 주기로 미는지 본다.
 *
 * <p>여기가 실패하면 원인은 크론 표현식이 아니라 "어느 주기를 어디까지 미는가"다.
 */
@DisplayName("ChoreRepeatRollService 반복 기간 갱신")
@ExtendWith(MockitoExtension.class)
class ChoreRepeatRollServiceTest {

	@Mock
	private ChoreRepository choreRepository;

	@InjectMocks
	private ChoreRepeatRollService choreRepeatRollService;

	@Test
	@DisplayName("일간은 오늘 하루로 민다")
	void 일간은_오늘_하루() {
		LocalDate today = LocalDate.of(2026, 9, 8);
		given(choreRepository.rollPeriod(eq(ChoreRepeatType.DAY), eq(today), eq(today), any())).willReturn(3);

		assertThat(choreRepeatRollService.roll(ChoreRepeatType.DAY, today)).isEqualTo(3);

		verify(choreRepository).rollPeriod(eq(ChoreRepeatType.DAY), eq(today), eq(today),
			any(OffsetDateTime.class));
	}

	@Test
	@DisplayName("주간은 그 주 일요일까지로 민다")
	void 주간은_그주_일요일까지() {
		// 2026-09-07 은 월요일. 크론은 월요일 자정에 돌고, 그 주 일요일은 09-13 이다.
		LocalDate monday = LocalDate.of(2026, 9, 7);

		choreRepeatRollService.roll(ChoreRepeatType.WEEK, monday);

		verify(choreRepository).rollPeriod(eq(ChoreRepeatType.WEEK), eq(monday),
			eq(LocalDate.of(2026, 9, 13)), any(OffsetDateTime.class));
	}

	@Test
	@DisplayName("월간은 그 달 말일까지로 민다")
	void 월간은_말일까지() {
		// 크론은 매월 1일 자정에 돈다.
		LocalDate first = LocalDate.of(2026, 7, 1);

		choreRepeatRollService.roll(ChoreRepeatType.MONTH, first);

		verify(choreRepository).rollPeriod(eq(ChoreRepeatType.MONTH), eq(first),
			eq(LocalDate.of(2026, 7, 31)), any(OffsetDateTime.class));
	}

	@Test
	@DisplayName("일회성은 크론이 건드리지 않는다")
	void 일회성은_밀지_않는다() {
		// 부모가 직접 정한 날짜를 크론이 바꾸면 조용히 기간이 달라진다.
		assertThatThrownBy(() -> choreRepeatRollService.roll(ChoreRepeatType.ONCE, LocalDate.of(2026, 9, 8)))
			.isInstanceOf(IllegalArgumentException.class);

		verify(choreRepository, never()).rollPeriod(any(), any(), any(), any());
	}
}
