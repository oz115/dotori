package com.bana.dotori.chore.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 1계층 - 집안일 생성 기본값과 수행 기간 판정만 검증한다.
 *
 * <p>스프링도 DB 도 없다. 이 테스트가 실패하면 원인은 무조건 {@link Chore} 안에 있다.
 */
@DisplayName("Chore 생성 기본값과 기간 판정")
class ChoreTest {

	@Nested
	@DisplayName("집안일을 등록할 때")
	class Create {

		@Test
		@DisplayName("지워지지 않은 상태로 시작한다")
		void 지워지지_않은_상태로_시작한다() {
			Chore chore = Chore.builder().title("설거지").rewardPoints(500).build();

			// 등록 직후에는 deleted_at 이 비어 있어야 목록에 나온다.
			assertThat(chore.isDeleted()).isFalse();
		}

		@Test
		@DisplayName("시작일을 비우면 오늘로 채운다")
		void 시작일을_비우면_오늘() {
			Chore chore = Chore.builder().title("설거지").rewardPoints(500).build();

			// DB 에 DEFAULT CURRENT_DATE 가 있지만 Hibernate 는 INSERT 에 모든 컬럼을 실으므로
			// null 을 그대로 두면 DEFAULT 가 아니라 NOT NULL 위반이 된다.
			assertThat(chore.getStartDate()).isEqualTo(LocalDate.now());
		}

		@Test
		@DisplayName("시작일을 주면 그 값을 쓴다")
		void 시작일을_주면_그_값() {
			LocalDate startDate = LocalDate.of(2026, 9, 1);

			Chore chore = Chore.builder().title("설거지").rewardPoints(500).startDate(startDate).build();

			assertThat(chore.getStartDate()).isEqualTo(startDate);
		}
	}

	@Nested
	@DisplayName("주기에서 종료일을 정할 때")
	class EndDate {

		@Test
		@DisplayName("일간은 그날 하루다")
		void 일간은_하루() {
			LocalDate tuesday = LocalDate.of(2026, 9, 8);

			assertThat(ChoreRepeatType.DAY.endDateFrom(tuesday)).isEqualTo(tuesday);
		}

		@Test
		@DisplayName("주간은 그 주 일요일까지다")
		void 주간은_그주_일요일() {
			// 2026-09-08 은 화요일, 그 주 일요일은 2026-09-13.
			assertThat(ChoreRepeatType.WEEK.endDateFrom(LocalDate.of(2026, 9, 8)))
				.isEqualTo(LocalDate.of(2026, 9, 13));
		}

		@Test
		@DisplayName("일요일에 주간으로 등록하면 그날 하루다")
		void 일요일_주간은_하루() {
			LocalDate sunday = LocalDate.of(2026, 9, 13);

			// nextOrSame 이라 일요일 당일은 그대로 끝난다. 다음 주까지 끌지 않는다.
			assertThat(ChoreRepeatType.WEEK.endDateFrom(sunday)).isEqualTo(sunday);
		}

		@Test
		@DisplayName("월간은 그 달 말일까지다")
		void 월간은_말일() {
			assertThat(ChoreRepeatType.MONTH.endDateFrom(LocalDate.of(2026, 7, 8)))
				.isEqualTo(LocalDate.of(2026, 7, 31));
		}

		@Test
		@DisplayName("월간은 달마다 말일이 다르다")
		void 월간_말일은_달마다_다르다() {
			assertThat(ChoreRepeatType.MONTH.endDateFrom(LocalDate.of(2026, 2, 1)))
				.isEqualTo(LocalDate.of(2026, 2, 28));
			// 2028 은 윤년이다.
			assertThat(ChoreRepeatType.MONTH.endDateFrom(LocalDate.of(2028, 2, 1)))
				.isEqualTo(LocalDate.of(2028, 2, 29));
		}
	}

	@Nested
	@DisplayName("수행 가능 기간을 볼 때")
	class Period {

		private final Chore bounded = Chore.builder()
			.startDate(LocalDate.of(2026, 9, 10))
			.endDate(LocalDate.of(2026, 9, 20))
			.build();

		@Test
		@DisplayName("시작일과 종료일은 포함한다")
		void 경계는_포함한다() {
			assertThat(bounded.isInPeriod(LocalDate.of(2026, 9, 10))).isTrue();
			assertThat(bounded.isInPeriod(LocalDate.of(2026, 9, 20))).isTrue();
		}

		@Test
		@DisplayName("기간 밖이면 수행할 수 없다")
		void 기간_밖은_거짓() {
			assertThat(bounded.isInPeriod(LocalDate.of(2026, 9, 9))).isFalse();
			assertThat(bounded.isInPeriod(LocalDate.of(2026, 9, 21))).isFalse();
		}

		@Test
		@DisplayName("종료일이 없으면 계속 수행할 수 있다")
		void 종료일이_없으면_계속() {
			Chore openEnded = Chore.builder().startDate(LocalDate.of(2026, 9, 10)).build();

			assertThat(openEnded.isInPeriod(LocalDate.of(2099, 1, 1))).isTrue();
		}
	}
}
