package com.bana.dotori.chore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.chore.dto.ChoreCompletionHistoryResponse;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.repository.PointTransactionRepository;

/**
 * 2계층 - 완료 내역 조회 (CHORE-03).
 *
 * <p>여기서 보는 것은 세 가지다. 지갑 원장을 읽는가, 기간 경계를 KST 로 잡는가,
 * 멱등키에서 집안일을 되찾아 제목을 붙이는가.
 */
@DisplayName("ChoreCompletionHistoryServiceImpl 완료 내역")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChoreCompletionHistoryServiceImplTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private static final Integer FAMILY_ID = 10;
	private static final Integer PARENT_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final Integer CHORE_ID = 7;

	@Mock
	private PointTransactionRepository pointTransactionRepository;

	@Mock
	private ChoreRepository choreRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ChoreAccessValidator accessValidator;

	@InjectMocks
	private ChoreCompletionHistoryServiceImpl historyService;

	private PointTransaction reward(int amount, String idempotencyKey) {
		return PointTransaction.record(CHILD_ID, PointTransactionReason.CHORE_REWARD,
			amount, amount, "집안일 보상: 설거지", idempotencyKey);
	}

	private Chore chore(Integer id, String title) {
		Chore chore = Chore.builder()
			.familyId(FAMILY_ID)
			.parentId(PARENT_ID)
			.childId(CHILD_ID)
			.title(title)
			.difficulty(ChoreDifficulty.NORMAL)
			.repeatType(ChoreRepeatType.DAY)
			.startDate(LocalDate.of(2026, 9, 9))
			.build();
		ReflectionTestUtils.setField(chore, "id", id);
		return chore;
	}

	private void givenChild() {
		User child = User.builder().loginId("c1").password("x").name("김아이")
			.email("c1@autotest.local").role(UserRole.CHILD).build();
		given(userRepository.findById(CHILD_ID)).willReturn(Optional.of(child));
	}

	private ChoreCompletionHistoryResponse get(LocalDate startDate, LocalDate endDate) {
		return historyService.getCompletions(PARENT_ID, FAMILY_ID, CHILD_ID, startDate, endDate);
	}

	@Nested
	@DisplayName("내역을 만들 때")
	class Items {

		@Test
		@DisplayName("지갑에 남은 실제 지급액을 쓴다")
		void 실제_지급액을_쓴다() {
			givenChild();
			given(pointTransactionRepository.search(eq(CHILD_ID), any(), any(), any(), any(), any(), anyInt()))
				.willReturn(List.of(reward(500, "chore:7:2026-09-09")));
			// 부모가 그 뒤 보상을 800 으로 고쳤다고 가정한다.
			given(choreRepository.findAllById(List.of(CHORE_ID))).willReturn(List.of(chore(CHORE_ID, "설거지")));

			ChoreCompletionHistoryResponse response = get(null, null);

			// chores.reward_points 를 읽었다면 800 이 나온다. 원장은 500 그대로다.
			assertThat(response.completions()).hasSize(1);
			assertThat(response.completions().getFirst().rewardPoints()).isEqualTo(500);
			assertThat(response.completions().getFirst().choreTitle()).isEqualTo("설거지");
			assertThat(response.completions().getFirst().childName()).isEqualTo("김아이");
		}

		@Test
		@DisplayName("집안일 보상만 읽는다")
		void 사유로_먼저_거른다() {
			givenChild();
			given(pointTransactionRepository.search(any(), any(), any(), any(), any(), any(), anyInt()))
				.willReturn(List.of());

			get(null, null);

			// 충전·지급은 멱등키를 클라이언트가 정하므로, 키 모양만 보고 거르면
			// 남의 충전이 집안일 보상으로 섞인다.
			ArgumentCaptor<PointTransactionReason> reason = ArgumentCaptor.forClass(PointTransactionReason.class);
			verify(pointTransactionRepository).search(any(), any(), any(), reason.capture(), any(), any(), anyInt());
			assertThat(reason.getValue()).isEqualTo(PointTransactionReason.CHORE_REWARD);
		}

		@Test
		@DisplayName("집안일 ID 를 한 번에 모아 조회한다")
		void 제목_조회는_한_번() {
			givenChild();
			given(pointTransactionRepository.search(any(), any(), any(), any(), any(), any(), anyInt()))
				.willReturn(List.of(
					reward(500, "chore:7:2026-09-09"),
					reward(500, "chore:7:2026-09-08"),
					reward(300, "chore:8:2026-09-09")));
			given(choreRepository.findAllById(any())).willReturn(List.of(chore(7, "설거지"), chore(8, "빨래")));

			ChoreCompletionHistoryResponse response = get(null, null);

			// 같은 집안일이 여러 주기에 걸쳐 나와도 조회는 한 번, ID 는 중복 없이 두 개다.
			ArgumentCaptor<List<Integer>> ids = ArgumentCaptor.captor();
			verify(choreRepository).findAllById(ids.capture());
			assertThat(ids.getValue()).containsExactly(7, 8);
			assertThat(response.completions()).hasSize(3);
		}

		@Test
		@DisplayName("멱등키가 집안일 형식이 아니면 제목 없이 넘어간다")
		void 형식이_아닌_키() {
			givenChild();
			given(pointTransactionRepository.search(any(), any(), any(), any(), any(), any(), anyInt()))
				.willReturn(List.of(reward(500, "user-supplied-key")));
			given(choreRepository.findAllById(List.of())).willReturn(List.of());

			ChoreCompletionHistoryResponse response = get(null, null);

			// 터지지 않고 넘어가야 한다. 사유가 CHORE_REWARD 인데 키가 어긋난 건 이상 상황이지만
			// 그 한 건 때문에 이력 화면 전체가 500 이 되면 안 된다.
			assertThat(response.completions()).hasSize(1);
			assertThat(response.completions().getFirst().choreTitle()).isNull();
			assertThat(response.completions().getFirst().rewardPoints()).isEqualTo(500);
		}
	}

	@Nested
	@DisplayName("기간을 정할 때")
	class Period {

		private ArgumentCaptor<OffsetDateTime> captureBound(int index) {
			ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
			ArgumentCaptor<OffsetDateTime> to = ArgumentCaptor.forClass(OffsetDateTime.class);
			verify(pointTransactionRepository).search(any(), from.capture(), to.capture(), any(), any(), any(),
				anyInt());
			return index == 0 ? from : to;
		}

		@Test
		@DisplayName("종료일 당일 승인 건이 포함되도록 다음 날 0시를 끝으로 잡는다")
		void 종료일_경계() {
			givenChild();
			given(pointTransactionRepository.search(any(), any(), any(), any(), any(), any(), anyInt()))
				.willReturn(List.of());

			get(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8));

			// "종료일 23:59:59" 로 자르면 그 뒤 밀리초에 생긴 거래가 빠진다.
			assertThat(captureBound(1).getValue())
				.isEqualTo(OffsetDateTime.parse("2026-09-09T00:00+09:00"));
		}

		@Test
		@DisplayName("시작일은 그날 0시 KST 다")
		void 시작일_경계() {
			givenChild();
			given(pointTransactionRepository.search(any(), any(), any(), any(), any(), any(), anyInt()))
				.willReturn(List.of());

			get(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8));

			assertThat(captureBound(0).getValue())
				.isEqualTo(OffsetDateTime.parse("2026-09-01T00:00+09:00"));
		}

		@Test
		@DisplayName("기간을 안 주면 최근 30일이다")
		void 기본값은_30일() {
			givenChild();
			given(pointTransactionRepository.search(any(), any(), any(), any(), any(), any(), anyInt()))
				.willReturn(List.of());

			get(null, null);

			LocalDate today = LocalDate.now(KST);
			assertThat(captureBound(0).getValue())
				.isEqualTo(today.minusDays(29).atStartOfDay(KST).toOffsetDateTime());
		}

		@Test
		@DisplayName("시작일이 종료일보다 늦으면 400이다")
		void 뒤집힌_기간() {
			assertThatThrownBy(() -> get(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 1)))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);
		}

		@Test
		@DisplayName("최신순으로 가져온다")
		void 최신순() {
			givenChild();
			given(pointTransactionRepository.search(any(), any(), any(), any(), any(), any(), anyInt()))
				.willReturn(List.of());

			get(null, null);

			ArgumentCaptor<Sort.Direction> direction = ArgumentCaptor.forClass(Sort.Direction.class);
			verify(pointTransactionRepository)
				.search(any(), any(), any(), any(), any(), direction.capture(), anyInt());
			assertThat(direction.getValue()).isEqualTo(Sort.Direction.DESC);
		}
	}

	@Nested
	@DisplayName("권한을 볼 때")
	class Access {

		@Test
		@DisplayName("형제 것을 보려 하면 403으로 막는다")
		void 형제_것은_볼_수_없다() {
			willThrow(new BusinessException(ErrorCode.FORBIDDEN))
				.given(accessValidator).validate(PARENT_ID, FAMILY_ID, CHILD_ID);

			assertThatThrownBy(() -> get(null, null))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
		}

		@Test
		@DisplayName("역할을 따로 좁히지 않는다")
		void 부모와_아이_모두_본다() {
			givenChild();
			given(pointTransactionRepository.search(any(), any(), any(), any(), any(), any(), anyInt()))
				.willReturn(List.of());

			get(null, null);

			// 이 화면은 공통이다. requireParent/requireChild 를 부르면 한쪽이 못 본다.
			verify(accessValidator).validate(PARENT_ID, FAMILY_ID, CHILD_ID);
		}
	}
}
