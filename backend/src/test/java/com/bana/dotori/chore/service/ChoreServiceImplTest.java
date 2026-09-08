package com.bana.dotori.chore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreUpdateRequest;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreRepeatType;
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
		assertThat(response.isActive()).isTrue();
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
	@DisplayName("기간을 비우면 오늘부터 기한 없이 반복한다")
	void 기간을_비우면_오늘부터() {
		given(choreRepository.save(any(Chore.class))).willAnswer(invocation -> invocation.getArgument(0));

		ChoreDetailResponse response = choreService.createChore(PARENT_ID, FAMILY_ID, CHILD_ID,
			new ChoreCreateRequest("설거지", null, 500, ChoreRepeatType.DAY, null, null));

		assertThat(response.startDate()).isEqualTo(LocalDate.now());
		assertThat(response.endDate()).isNull();
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
		given(choreRepository.findAllByFamilyIdAndChildIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(
			FAMILY_ID, CHILD_ID)).willReturn(List.of(chore(CHORE_ID, CHILD_ID)));

		assertThat(choreService.getChores(CHILD_ID, FAMILY_ID, CHILD_ID).chores())
			.extracting(summary -> summary.choreId())
			.containsExactly(CHORE_ID);
	}

	@Test
	@DisplayName("부모는 지목한 아이의 집안일 목록을 본다")
	void 부모는_지목한_아이_목록을_본다() {
		given(choreRepository.findAllByFamilyIdAndChildIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(
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
			new ChoreUpdateRequest(null, null, 700, null, null, null));

		assertThat(response.rewardPoints()).isEqualTo(700);
		assertThat(response.title()).isEqualTo("설거지");
		assertThat(response.repeatType()).isEqualTo(ChoreRepeatType.DAY);
	}

	@Test
	@DisplayName("아이는 수정할 수 없다")
	void 아이는_수정할_수_없다() {
		assertThatThrownBy(() -> choreService.updateChore(CHILD_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, 700, null, null, null)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("종료일만 앞으로 당겨 기간이 뒤집히면 400이다")
	void 수정으로_기간이_뒤집히면_400() {
		Chore chore = Chore.builder()
			.familyId(FAMILY_ID).parentId(PARENT_ID).childId(CHILD_ID)
			.title("설거지").rewardPoints(500).repeatType(ChoreRepeatType.DAY)
			.startDate(LocalDate.of(2026, 9, 10))
			.build();
		ReflectionTestUtils.setField(chore, "id", CHORE_ID);
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(chore));

		// 요청 DTO 는 시작일의 현재 값을 모르므로 합쳐진 상태를 아는 엔티티가 잡는다.
		assertThatThrownBy(() -> choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, null, null, null, LocalDate.of(2026, 9, 9))))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("삭제는 비활성화이고 행을 지우지 않는다")
	void 삭제는_비활성화다() {
		Chore chore = chore(CHORE_ID, CHILD_ID);
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(chore));

		choreService.deactivateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID);

		assertThat(chore.isActive()).isFalse();
		// 완료 이력이 chore_id 를 참조하므로 진짜로 지우면 지급한 보상의 근거가 사라진다.
		verify(choreRepository, never()).delete(any());
		verify(choreRepository, never()).deleteById(any());
	}

	@Test
	@DisplayName("아이는 삭제할 수 없다")
	void 아이는_삭제할_수_없다() {
		assertThatThrownBy(() -> choreService.deactivateChore(CHILD_ID, FAMILY_ID, CHILD_ID, CHORE_ID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("경로의 childId와 담당이 어긋나면 수정도 404다")
	void 수정도_경로와_담당을_대조한다() {
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID))
			.willReturn(Optional.of(chore(CHORE_ID, SIBLING_ID)));

		assertThatThrownBy(() -> choreService.updateChore(PARENT_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreUpdateRequest(null, null, 700, null, null, null)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_NOT_FOUND);
	}

	private ChoreCreateRequest request() {
		return new ChoreCreateRequest("설거지", "저녁 먹고 나서", 500, ChoreRepeatType.DAY,
			LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
	}

	private Chore chore(Integer choreId, Integer childId) {
		Chore chore = Chore.builder()
			.familyId(FAMILY_ID)
			.parentId(PARENT_ID)
			.childId(childId)
			.title("설거지")
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
