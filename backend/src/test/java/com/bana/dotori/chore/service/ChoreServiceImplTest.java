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

	private ChoreCreateRequest request(Integer childId) {
		return new ChoreCreateRequest(childId, "설거지", "저녁 먹고 나서", 500, ChoreRepeatType.DAY,
			LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
	}

	private void givenParent() {
		given(userRepository.findById(PARENT_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
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
