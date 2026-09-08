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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.mapper.ChoreMapper;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

/**
 * 2계층 - 집안일 생성이 권한과 가족 경계를 지키는지 본다.
 *
 * <p>여기가 실패하면 원인은 저장이 아니라 "누가 누구에게 집안일을 줄 수 있는가"다.
 */
@DisplayName("ChoreServiceImpl 집안일 생성")
@ExtendWith(MockitoExtension.class)
class ChoreServiceImplTest {

	private static final Integer PARENT_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final Integer FAMILY_ID = 10;

	@Mock
	private ChoreRepository choreRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Spy
	private ChoreMapper choreMapper;

	@InjectMocks
	private ChoreServiceImpl choreService;

	@Test
	@DisplayName("부모가 자기 자녀에게 집안일을 등록한다")
	void 부모가_등록한다() {
		givenParent();
		givenMyChildren(CHILD_ID);
		given(choreRepository.save(any(Chore.class))).willAnswer(invocation -> invocation.getArgument(0));

		ChoreDetailResponse response = choreService.createChore(PARENT_ID, request(CHILD_ID));

		ArgumentCaptor<Chore> captor = ArgumentCaptor.forClass(Chore.class);
		verify(choreRepository).save(captor.capture());
		Chore saved = captor.getValue();
		// familyId·parentId 는 요청이 아니라 인증 주체에서 와야 한다.
		assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(saved.getParentId()).isEqualTo(PARENT_ID);
		assertThat(saved.getChildId()).isEqualTo(CHILD_ID);
		assertThat(saved.getRewardPoints()).isEqualTo(500);
		assertThat(saved.getRepeatType()).isEqualTo(ChoreRepeatType.DAY);
		assertThat(response.childId()).isEqualTo(CHILD_ID);
		assertThat(response.isActive()).isTrue();
	}

	@Test
	@DisplayName("아이가 등록을 시도하면 403으로 막고 저장하지 않는다")
	void 아이는_등록할_수_없다() {
		given(userRepository.findById(PARENT_ID)).willReturn(Optional.of(user(UserRole.CHILD)));

		assertThatThrownBy(() -> choreService.createChore(PARENT_ID, request(CHILD_ID)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

		verify(choreRepository, never()).save(any());
	}

	@Test
	@DisplayName("없는 회원이 호출하면 401로 막는다")
	void 없는_회원은_401() {
		given(userRepository.findById(PARENT_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> choreService.createChore(PARENT_ID, request(CHILD_ID)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

		verify(choreRepository, never()).save(any());
	}

	@Test
	@DisplayName("내 가족이 아닌 아이에게는 등록할 수 없다")
	void 남의_자녀에게는_등록할_수_없다() {
		givenParent();
		givenMyChildren(CHILD_ID);

		// 요청 본문의 childId 를 그대로 믿으면 남의 가족 아이에게 집안일이 심긴다.
		assertThatThrownBy(() -> choreService.createChore(PARENT_ID, request(999)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

		verify(choreRepository, never()).save(any());
	}

	@Test
	@DisplayName("자녀가 없는 부모는 등록할 수 없다")
	void 자녀가_없으면_등록할_수_없다() {
		givenParent();
		given(familyMemberRepository.findConnectedChildren(PARENT_ID, UserRole.CHILD)).willReturn(List.of());

		assertThatThrownBy(() -> choreService.createChore(PARENT_ID, request(CHILD_ID)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("기간을 비우면 오늘부터 기한 없이 반복한다")
	void 기간을_비우면_오늘부터() {
		givenParent();
		givenMyChildren(CHILD_ID);
		given(choreRepository.save(any(Chore.class))).willAnswer(invocation -> invocation.getArgument(0));

		ChoreDetailResponse response = choreService.createChore(PARENT_ID, new ChoreCreateRequest(
			CHILD_ID, "설거지", null, 500, ChoreRepeatType.DAY, null, null));

		assertThat(response.startDate()).isEqualTo(LocalDate.now());
		assertThat(response.endDate()).isNull();
	}

	@Test
	@DisplayName("부모는 가족 전체의 집안일을 본다")
	void 부모는_가족_전체를_본다() {
		givenUser(PARENT_ID, UserRole.PARENT);
		givenMyFamily(PARENT_ID);
		given(choreRepository.findAllByFamilyIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(FAMILY_ID))
			.willReturn(List.of(chore(1, CHILD_ID), chore(2, 3)));

		assertThat(choreService.getChores(PARENT_ID, null).chores()).hasSize(2);
	}

	@Test
	@DisplayName("부모가 childId를 넘기면 그 자녀 몫만 본다")
	void 부모는_자녀별로_걸러_본다() {
		givenUser(PARENT_ID, UserRole.PARENT);
		givenMyFamily(PARENT_ID);
		given(choreRepository.findAllByFamilyIdAndChildIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(
			FAMILY_ID, CHILD_ID)).willReturn(List.of(chore(1, CHILD_ID)));

		assertThat(choreService.getChores(PARENT_ID, CHILD_ID).chores())
			.extracting(summary -> summary.childId())
			.containsExactly(CHILD_ID);
	}

	@Test
	@DisplayName("아이는 남의 childId를 넘겨도 자기 몫만 본다")
	void 아이는_자기_몫만_본다() {
		givenUser(CHILD_ID, UserRole.CHILD);
		givenMyFamily(CHILD_ID);
		given(choreRepository.findAllByFamilyIdAndChildIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(
			FAMILY_ID, CHILD_ID)).willReturn(List.of(chore(1, CHILD_ID)));

		// 쿼리 파라미터로 형제 ID 를 넣어도 자기 것만 나와야 한다.
		assertThat(choreService.getChores(CHILD_ID, 3).chores()).hasSize(1);
		verify(choreRepository, never())
			.findAllByFamilyIdAndChildIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(FAMILY_ID, 3);
	}

	@Test
	@DisplayName("가족에 속하지 않으면 목록을 볼 수 없다")
	void 가족이_없으면_목록도_없다() {
		givenUser(PARENT_ID, UserRole.PARENT);
		given(familyMemberRepository.findActiveFamilyIdByUserId(PARENT_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> choreService.getChores(PARENT_ID, null))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("부모는 가족의 집안일 상세를 본다")
	void 부모는_상세를_본다() {
		givenUser(PARENT_ID, UserRole.PARENT);
		givenMyFamily(PARENT_ID);
		given(choreRepository.findByIdAndFamilyId(7, FAMILY_ID)).willReturn(Optional.of(chore(7, CHILD_ID)));

		assertThat(choreService.getChore(PARENT_ID, 7).choreId()).isEqualTo(7);
	}

	@Test
	@DisplayName("다른 가족의 집안일 상세는 404다")
	void 남의_가족_상세는_404() {
		givenUser(PARENT_ID, UserRole.PARENT);
		givenMyFamily(PARENT_ID);
		given(choreRepository.findByIdAndFamilyId(7, FAMILY_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> choreService.getChore(PARENT_ID, 7))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_NOT_FOUND);
	}

	@Test
	@DisplayName("아이는 형제에게 배정된 집안일 상세를 볼 수 없다")
	void 아이는_형제_것을_볼_수_없다() {
		givenUser(CHILD_ID, UserRole.CHILD);
		givenMyFamily(CHILD_ID);
		given(choreRepository.findByIdAndFamilyId(7, FAMILY_ID)).willReturn(Optional.of(chore(7, 3)));

		// 403 이면 그 choreId 가 존재한다는 사실이 새어 나간다.
		assertThatThrownBy(() -> choreService.getChore(CHILD_ID, 7))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_NOT_FOUND);
	}

	@Test
	@DisplayName("아이는 자기에게 배정된 집안일 상세는 볼 수 있다")
	void 아이는_자기_것은_본다() {
		givenUser(CHILD_ID, UserRole.CHILD);
		givenMyFamily(CHILD_ID);
		given(choreRepository.findByIdAndFamilyId(7, FAMILY_ID)).willReturn(Optional.of(chore(7, CHILD_ID)));

		assertThat(choreService.getChore(CHILD_ID, 7).childId()).isEqualTo(CHILD_ID);
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

	private ChoreCreateRequest request(Integer childId) {
		return new ChoreCreateRequest(childId, "설거지", "저녁 먹고 나서", 500, ChoreRepeatType.DAY,
			LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
	}

	private void givenParent() {
		given(userRepository.findById(PARENT_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
	}

	private void givenUser(Integer userId, UserRole role) {
		User target = user(role);
		ReflectionTestUtils.setField(target, "id", userId);
		given(userRepository.findById(userId)).willReturn(Optional.of(target));
	}

	private void givenMyFamily(Integer userId) {
		given(familyMemberRepository.findActiveFamilyIdByUserId(userId)).willReturn(Optional.of(FAMILY_ID));
	}

	private void givenMyChildren(Integer... childIds) {
		given(familyMemberRepository.findConnectedChildren(PARENT_ID, UserRole.CHILD))
			.willReturn(List.of(childIds).stream().map(this::member).toList());
	}

	private User user(UserRole role) {
		User user = User.builder()
			.loginId("login")
			.password("pw")
			.name("이름")
			.email("test@autotest.local")
			.role(role)
			.build();
		ReflectionTestUtils.setField(user, "id", PARENT_ID);
		return user;
	}

	private FamilyMember member(Integer childId) {
		User child = User.builder()
			.loginId("child" + childId)
			.password("pw")
			.name("아이")
			.email("child" + childId + "@autotest.local")
			.role(UserRole.CHILD)
			.build();
		ReflectionTestUtils.setField(child, "id", childId);

		Family family = Family.builder().name("도토리네").createdBy(PARENT_ID).build();
		ReflectionTestUtils.setField(family, "id", FAMILY_ID);

		return FamilyMember.builder().family(family).user(child).build();
	}
}
