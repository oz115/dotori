package com.bana.dotori.chore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreUpdateRequest;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.event.ChoreCreatedEvent;
import com.bana.dotori.chore.mapper.ChoreMapper;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

/**
 * 2계층 - 집안일 서비스가 경로의 familyId·childId 를 검증하는지 본다.
 *
 * <p>경로에서 온 값은 믿을 수 없다. 여기가 실패하면 원인은 저장이나 조회가 아니라
 * "누가 어느 가족의 어느 아이 것을 다룰 수 있는가"다.
 */
@DisplayName("ChoreServiceImpl 권한과 가족 경계")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChoreServiceImplTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private static final Integer PARENT_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final Integer SIBLING_ID = 3;
	private static final Integer FAMILY_ID = 10;
	private static final Integer CHORE_ID = 7;

	@Mock
	private ChoreRepository choreRepository;

	@Mock
	private FamilyRepository familyRepository;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Spy
	private ChoreMapper choreMapper;

	@InjectMocks
	private ChoreServiceImpl choreService;

	@BeforeEach
	void setUpFamily() {
		Family family = Family.builder().name("도토리네").createdBy(PARENT_ID).build();
		ReflectionTestUtils.setField(family, "id", FAMILY_ID);
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID)).willReturn(Optional.of(family));

		givenUser(PARENT_ID, UserRole.PARENT);
		givenUser(CHILD_ID, UserRole.CHILD);
		givenUser(SIBLING_ID, UserRole.CHILD);
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, SIBLING_ID)).willReturn(true);
		givenFamilyChild(CHILD_ID);
		givenFamilyChild(SIBLING_ID);
	}

	@Test
	@DisplayName("부모가 자기 가족 아이에게 집안일을 등록한다")
	void 부모가_등록한다() {
		given(choreRepository.save(any(Chore.class))).willAnswer(invocation -> invocation.getArgument(0));

		ChoreDetailResponse response = choreService.createChore(PARENT_ID, FAMILY_ID, CHILD_ID, request());

		ArgumentCaptor<Chore> captor = ArgumentCaptor.forClass(Chore.class);
		verify(choreRepository).save(captor.capture());
		Chore saved = captor.getValue();
		// familyId·childId 는 경로에서, parentId 는 인증 주체에서 온다.
		assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(saved.getParentId()).isEqualTo(PARENT_ID);
		assertThat(saved.getChildId()).isEqualTo(CHILD_ID);
		assertThat(response.deletedAt()).isNull();
	}

	@Test
	@DisplayName("아이가 등록을 시도하면 403으로 막고 저장하지 않는다")
	void 아이는_등록할_수_없다() {
		assertThatThrownBy(() -> choreService.createChore(CHILD_ID, FAMILY_ID, CHILD_ID, request()))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

		verify(choreRepository, never()).save(any());
	}

	@Test
	@DisplayName("보상은 등록 시점에 비어 있고 AI 산정 이벤트가 발행된다")
	void 보상은_AI가_채운다() {
		given(choreRepository.save(any(Chore.class))).willAnswer(invocation -> invocation.getArgument(0));

		ChoreDetailResponse response = choreService.createChore(PARENT_ID, FAMILY_ID, CHILD_ID, request());

		// 부모는 보상을 정하지 않는다. AI 가 value 를 산정하고 주간 용돈을 곱해 채운다.
		assertThat(response.value()).isNull();
		assertThat(response.rewardPoints()).isNull();

		ArgumentCaptor<ChoreCreatedEvent> captor = ArgumentCaptor.forClass(ChoreCreatedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().difficulty()).isEqualTo(ChoreDifficulty.NORMAL);
		assertThat(captor.getValue().familyId()).isEqualTo(FAMILY_ID);
	}

	@Test
	@DisplayName("시작일은 오늘이고 종료일은 주기에서 계산된다")
	void 기간은_주기에서_정해진다() {
		given(choreRepository.save(any(Chore.class))).willAnswer(invocation -> invocation.getArgument(0));

		ChoreDetailResponse response = choreService.createChore(PARENT_ID, FAMILY_ID, CHILD_ID,
			new ChoreCreateRequest("설거지", null, ChoreDifficulty.NORMAL, ChoreRepeatType.MONTH, null, null));

		LocalDate today = LocalDate.now(KST);
		assertThat(response.startDate()).isEqualTo(today);
		// 월간이면 그 달 말일까지다. 부모는 날짜를 직접 넣지 않는다.
		assertThat(response.endDate()).isEqualTo(today.with(TemporalAdjusters.lastDayOfMonth()));
	}

	@Test
	@DisplayName("일회성이면 부모가 넣은 기간을 그대로 쓴다")
	void 일회성은_받은_기간을_쓴다() {
		given(choreRepository.save(any(Chore.class))).willAnswer(invocation -> invocation.getArgument(0));
		LocalDate start = LocalDate.of(2026, 9, 10);
		LocalDate end = LocalDate.of(2026, 9, 20);

		ChoreDetailResponse response = choreService.createChore(PARENT_ID, FAMILY_ID, CHILD_ID,
			new ChoreCreateRequest("대청소", null, ChoreDifficulty.HARD, ChoreRepeatType.ONCE, start, end));

		assertThat(response.startDate()).isEqualTo(start);
		assertThat(response.endDate()).isEqualTo(end);
	}

	@Test
	@DisplayName("반복이면 요청에 날짜가 있어도 무시하고 주기로 계산한다")
	void 반복은_받은_날짜를_무시한다() {
		given(choreRepository.save(any(Chore.class))).willAnswer(invocation -> invocation.getArgument(0));

		ChoreDetailResponse response = choreService.createChore(PARENT_ID, FAMILY_ID, CHILD_ID,
			new ChoreCreateRequest("설거지", null, ChoreDifficulty.NORMAL, ChoreRepeatType.DAY,
				LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31)));

		// 크론이 주기마다 기간을 다시 계산해 밀기 때문에, 첫 주기만 다르게 두면 안 된다.
		LocalDate today = LocalDate.now(KST);
		assertThat(response.startDate()).isEqualTo(today);
		assertThat(response.endDate()).isEqualTo(today);
	}

	@Test
	@DisplayName("수정에서는 AI 산정 이벤트를 발행하지 않는다")
	void 수정은_AI를_다시_부르지_않는다() {
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID))
			.willReturn(Optional.of(chore(CHORE_ID, CHILD_ID)));

		choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, 700, null, null, null));

		// 부모가 직접 고친 보상을 AI 가 다시 덮어쓰면 수정이 되돌려진 것처럼 보인다.
		verify(eventPublisher, never()).publishEvent(any(ChoreCreatedEvent.class));
	}

	@Test
	@DisplayName("없는 가족이면 404다")
	void 없는_가족은_404() {
		given(familyRepository.findByIdAndDeletedAtIsNull(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> choreService.getChores(PARENT_ID, 99, CHILD_ID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("그 가족의 구성원이 아니면 403이 아니라 404다")
	void 남의_가족은_404() {
		Integer outsider = 99;
		givenUser(outsider, UserRole.PARENT);
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, outsider)).willReturn(false);

		// 403 을 주면 그 familyId 가 실재한다는 사실이 새어 나간다.
		assertThatThrownBy(() -> choreService.getChores(outsider, FAMILY_ID, CHILD_ID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("경로의 childId가 그 가족 자녀가 아니면 404다")
	void 남의_자녀는_404() {
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, 99))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> choreService.getChores(PARENT_ID, FAMILY_ID, 99))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("경로의 childId가 부모면 404다")
	void 부모를_childId로_넣으면_404() {
		FamilyMember parentMember = member(PARENT_ID, UserRole.PARENT);
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, PARENT_ID))
			.willReturn(Optional.of(parentMember));

		assertThatThrownBy(() -> choreService.getChores(PARENT_ID, FAMILY_ID, PARENT_ID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("아이가 형제의 집안일을 보려 하면 403이다")
	void 아이는_형제_것을_볼_수_없다() {
		// 형제의 존재는 이미 아는 사실이라 숨길 것이 없고, 권한 문제임을 알려 주는 편이 낫다.
		assertThatThrownBy(() -> choreService.getChores(CHILD_ID, FAMILY_ID, SIBLING_ID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("아이는 자기 집안일 목록을 본다")
	void 아이는_자기_목록을_본다() {
		given(choreRepository.findAllByFamilyIdAndChildIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
			FAMILY_ID, CHILD_ID)).willReturn(List.of(chore(CHORE_ID, CHILD_ID)));

		assertThat(choreService.getChores(CHILD_ID, FAMILY_ID, CHILD_ID).chores())
			.extracting(summary -> summary.choreId())
			.containsExactly(CHORE_ID);
	}

	@Test
	@DisplayName("부모는 지목한 아이의 집안일 목록을 본다")
	void 부모는_지목한_아이_목록을_본다() {
		given(choreRepository.findAllByFamilyIdAndChildIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
			FAMILY_ID, SIBLING_ID)).willReturn(List.of(chore(8, SIBLING_ID)));

		assertThat(choreService.getChores(PARENT_ID, FAMILY_ID, SIBLING_ID).chores()).hasSize(1);
	}

	@Test
	@DisplayName("상세를 조회한다")
	void 상세를_조회한다() {
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID))
			.willReturn(Optional.of(chore(CHORE_ID, CHILD_ID)));

		assertThat(choreService.getChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID).choreId())
			.isEqualTo(CHORE_ID);
	}

	@Test
	@DisplayName("없는 집안일이면 404다")
	void 없는_집안일은_404() {
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> choreService.getChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_NOT_FOUND);
	}

	@Test
	@DisplayName("경로의 childId와 집안일의 담당이 어긋나면 404다")
	void 경로와_담당이_어긋나면_404() {
		// 같은 가족이라도 경로가 지목한 아이 것이 아니면 없는 것으로 본다.
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID))
			.willReturn(Optional.of(chore(CHORE_ID, SIBLING_ID)));

		assertThatThrownBy(() -> choreService.getChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_NOT_FOUND);
	}

	@Test
	@DisplayName("보낸 필드만 바뀌고 나머지는 그대로다")
	void 수정은_보낸_필드만_바꾼다() {
		Chore chore = chore(CHORE_ID, CHILD_ID);
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(chore));

		ChoreDetailResponse response = choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, 700, null, null, null));

		assertThat(response.rewardPoints()).isEqualTo(700);
		assertThat(response.title()).isEqualTo("설거지");
		assertThat(response.repeatType()).isEqualTo(ChoreRepeatType.DAY);
	}

	@Test
	@DisplayName("일회성을 반복으로 바꾸면 기간을 오늘 기준으로 다시 계산한다")
	void 일회성에서_반복으로_바꾸면_기간_재계산() {
		Chore once = onceChore(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20));
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(once));

		ChoreDetailResponse response = choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, null, ChoreRepeatType.DAY, null, null));

		// 기간을 그대로 두면 "매일 반복인데 9/20 에 끝나는" 상태가 되고,
		// 크론은 end_date 가 지난 것만 밀기 때문에 그날이 와야 정상으로 돌아온다.
		LocalDate today = LocalDate.now(KST);
		assertThat(response.repeatType()).isEqualTo(ChoreRepeatType.DAY);
		assertThat(response.startDate()).isEqualTo(today);
		assertThat(response.endDate()).isEqualTo(today);
	}

	@Test
	@DisplayName("반복 주기를 다른 반복으로 바꾸면 새 주기로 다시 계산한다")
	void 반복에서_다른_반복으로_바꾸면_기간_재계산() {
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID))
			.willReturn(Optional.of(chore(CHORE_ID, CHILD_ID)));

		ChoreDetailResponse response = choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, null, ChoreRepeatType.MONTH, null, null));

		LocalDate today = LocalDate.now(KST);
		assertThat(response.startDate()).isEqualTo(today);
		assertThat(response.endDate()).isEqualTo(today.with(TemporalAdjusters.lastDayOfMonth()));
	}

	@Test
	@DisplayName("반복으로 바꿀 때 보낸 날짜는 무시한다")
	void 반복으로_바꿀_때_받은_날짜는_무시() {
		Chore once = onceChore(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20));
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(once));

		ChoreDetailResponse response = choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, null, ChoreRepeatType.DAY,
				LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31)));

		assertThat(response.startDate()).isEqualTo(LocalDate.now(KST));
	}

	@Test
	@DisplayName("반복을 일회성으로 바꿀 때 기간을 안 주면 400이다")
	void 일회성으로_바꿀_때_기간이_없으면_400() {
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID))
			.willReturn(Optional.of(chore(CHORE_ID, CHILD_ID)));

		// 시작일은 오늘로 둘 수 있지만 종료일은 계산할 근거가 없다.
		assertThatThrownBy(() -> choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, null, ChoreRepeatType.ONCE, null, null)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("반복을 일회성으로 바꾸면서 기간을 주면 그 기간을 쓴다")
	void 일회성으로_바꾸면_받은_기간을_쓴다() {
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID))
			.willReturn(Optional.of(chore(CHORE_ID, CHILD_ID)));
		LocalDate start = LocalDate.of(2026, 10, 1);
		LocalDate end = LocalDate.of(2026, 10, 5);

		ChoreDetailResponse response = choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, null, ChoreRepeatType.ONCE, start, end));

		assertThat(response.repeatType()).isEqualTo(ChoreRepeatType.ONCE);
		assertThat(response.startDate()).isEqualTo(start);
		assertThat(response.endDate()).isEqualTo(end);
	}

	@Test
	@DisplayName("반복 주기를 그대로 두면 기간을 건드리지 않는다")
	void 주기가_같으면_기간은_그대로() {
		Chore chore = chore(CHORE_ID, CHILD_ID);
		LocalDate before = chore.getStartDate();
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(chore));

		// 기간은 크론이 관리한다. 보상만 고쳤는데 기간이 바뀌면 안 된다.
		ChoreDetailResponse response = choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, 700, ChoreRepeatType.DAY,
				LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31)));

		assertThat(response.startDate()).isEqualTo(before);
		assertThat(response.rewardPoints()).isEqualTo(700);
	}

	@Test
	@DisplayName("일회성의 기간만 고칠 수 있고 한쪽만 보내도 된다")
	void 일회성_기간만_고친다() {
		Chore once = onceChore(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20));
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(once));

		ChoreDetailResponse response = choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, null, null, null, LocalDate.of(2026, 9, 25)));

		assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 9, 10));
		assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 25));
	}

	@Test
	@DisplayName("아이는 수정할 수 없다")
	void 아이는_수정할_수_없다() {
		assertThatThrownBy(() -> choreService.updateChore(CHILD_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, 700, null, null, null)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("일회성의 종료일만 앞으로 당겨 기간이 뒤집히면 400이다")
	void 수정으로_기간이_뒤집히면_400() {
		Chore once = onceChore(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20));
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(once));

		// 요청 DTO 는 시작일의 현재 값을 모르므로 합쳐진 상태를 아는 엔티티가 잡는다.
		// 반복은 기간을 계산으로 정하므로 뒤집힐 수 없어 이 검사가 필요 없다.
		assertThatThrownBy(() -> choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, null, null, null, LocalDate.of(2026, 9, 9))))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("삭제는 소프트 삭제이고 행을 지우지 않는다")
	void 삭제는_소프트_삭제다() {
		Chore chore = chore(CHORE_ID, CHILD_ID);
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(chore));

		choreService.deleteChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID);

		assertThat(chore.isDeleted()).isTrue();
		// 완료 이력이 chore_id 를 참조하므로 진짜로 지우면 지급한 보상의 근거가 사라진다.
		verify(choreRepository, never()).delete(any());
		verify(choreRepository, never()).deleteById(any());
	}

	@Test
	@DisplayName("아이는 삭제할 수 없다")
	void 아이는_삭제할_수_없다() {
		assertThatThrownBy(() -> choreService.deleteChore(CHILD_ID, FAMILY_ID, CHILD_ID, CHORE_ID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("경로의 childId와 담당이 어긋나면 수정도 404다")
	void 수정도_경로와_담당을_대조한다() {
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID))
			.willReturn(Optional.of(chore(CHORE_ID, SIBLING_ID)));

		assertThatThrownBy(() -> choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, 700, null, null, null)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_NOT_FOUND);
	}

	private ChoreCreateRequest request() {
		return new ChoreCreateRequest("설거지", "저녁 먹고 나서", ChoreDifficulty.NORMAL, ChoreRepeatType.DAY, null, null);
	}

	private Chore onceChore(LocalDate startDate, LocalDate endDate) {
		Chore chore = Chore.builder()
			.familyId(FAMILY_ID)
			.parentId(PARENT_ID)
			.childId(CHILD_ID)
			.title("대청소")
			.difficulty(ChoreDifficulty.HARD)
			.repeatType(ChoreRepeatType.ONCE)
			.startDate(startDate)
			.endDate(endDate)
			.build();
		ReflectionTestUtils.setField(chore, "id", CHORE_ID);
		return chore;
	}

	private Chore chore(Integer choreId, Integer childId) {
		Chore chore = Chore.builder()
			.familyId(FAMILY_ID)
			.parentId(PARENT_ID)
			.childId(childId)
			.title("설거지")
			.difficulty(ChoreDifficulty.NORMAL)
			.rewardPoints(500)
			.repeatType(ChoreRepeatType.DAY)
			.build();
		ReflectionTestUtils.setField(chore, "id", choreId);
		return chore;
	}

	private void givenUser(Integer userId, UserRole role) {
		given(userRepository.findById(userId)).willReturn(Optional.of(user(userId, role)));
	}

	private void givenFamilyChild(Integer childId) {
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, childId))
			.willReturn(Optional.of(member(childId, UserRole.CHILD)));
	}

	private User user(Integer userId, UserRole role) {
		User user = User.builder()
			.loginId("login" + userId)
			.password("pw")
			.name("이름")
			.email("user" + userId + "@autotest.local")
			.role(role)
			.build();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private FamilyMember member(Integer userId, UserRole role) {
		Family family = Family.builder().name("도토리네").createdBy(PARENT_ID).build();
		ReflectionTestUtils.setField(family, "id", FAMILY_ID);
		return FamilyMember.builder().family(family).user(user(userId, role)).build();
	}
}
