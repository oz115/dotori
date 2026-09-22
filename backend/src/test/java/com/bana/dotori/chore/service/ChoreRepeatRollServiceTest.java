package com.bana.dotori.chore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.chore.entity.ChoreCompletionRequest;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;
import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.repository.ChoreCompletionRequestRepository;
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

	@Mock
	private ChoreCompletionRequestRepository completionRequestRepository;

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

	@Test
	@DisplayName("주기가 끝나도록 처리되지 않은 요청은 자동으로 거절 처리한다")
	void 방치된_요청은_자동_거절() {
		// 대기 요청이 남아 있으면 같은 할 일에 새 요청을 못 만든다(PENDING_REQUEST_EXISTS).
		// 정리하지 않으면 그 반복 할 일은 새 주기가 와도 아이가 다시 할 수 없다 (QA).
		LocalDate today = LocalDate.of(2026, 9, 8);
		ChoreCompletionRequest stale = pendingRequest();
		given(completionRequestRepository.findStalePending(ChoreRepeatType.DAY, today))
			.willReturn(List.of(stale));

		choreRepeatRollService.roll(ChoreRepeatType.DAY, today);

		assertThat(stale.getStatus()).isEqualTo(ChoreCompletionStatus.REJECTED);
		assertThat(stale.getProcessedAt()).isNotNull();
	}

	@Test
	@DisplayName("자동 거절은 기간을 밀기 전에 한다 — 새 기간으로 민 뒤면 대상이 사라진다")
	void 거절이_기간_갱신보다_먼저() {
		LocalDate today = LocalDate.of(2026, 9, 8);
		given(completionRequestRepository.findStalePending(ChoreRepeatType.DAY, today))
			.willReturn(List.of(pendingRequest()));

		choreRepeatRollService.roll(ChoreRepeatType.DAY, today);

		InOrder inOrder = inOrder(completionRequestRepository, choreRepository);
		inOrder.verify(completionRequestRepository).findStalePending(ChoreRepeatType.DAY, today);
		inOrder.verify(choreRepository).rollPeriod(any(), any(), any(), any());
	}

	@Test
	@DisplayName("정리할 요청이 없어도 기간 갱신은 그대로 한다")
	void 정리할_것이_없어도_민다() {
		LocalDate today = LocalDate.of(2026, 9, 8);
		given(completionRequestRepository.findStalePending(ChoreRepeatType.DAY, today))
			.willReturn(List.of());

		choreRepeatRollService.roll(ChoreRepeatType.DAY, today);

		verify(choreRepository).rollPeriod(eq(ChoreRepeatType.DAY), eq(today), eq(today),
			any(OffsetDateTime.class));
	}

	@Test
	@DisplayName("일회성은 요청 정리도 하지 않는다")
	void 일회성은_정리도_안_한다() {
		assertThatThrownBy(() -> choreRepeatRollService.roll(ChoreRepeatType.ONCE, LocalDate.of(2026, 9, 8)))
			.isInstanceOf(IllegalArgumentException.class);

		verify(completionRequestRepository, never()).findStalePending(any(), any());
	}

	private ChoreCompletionRequest pendingRequest() {
		return ChoreCompletionRequest.builder()
			.choreId(1)
			.childId(2)
			.description("다 했어요")
			.build();
	}
}