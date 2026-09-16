package com.bana.dotori.quiz.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

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
 * 2계층 - 퀴즈 API 경로의 인가 규칙. {@code ChoreAccessValidatorTest}와 같은 규칙을
 * 그대로 옮겨 쓴다 — 가족·자녀 경로 검증 자체는 도메인이 달라도 동일하다.
 */
@DisplayName("QuizAccessValidator 퀴즈 인가 규칙")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuizAccessValidatorTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer PARENT_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final Integer SIBLING_ID = 3;
	private static final Integer OUTSIDER_ID = 99;

	@Mock
	private FamilyRepository familyRepository;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private QuizAccessValidator accessValidator;

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

	private void givenUser(Integer userId, UserRole role) {
		given(userRepository.findById(userId)).willReturn(Optional.of(user(userId, role)));
	}

	private void givenFamilyChild(Integer childId) {
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, childId))
			.willReturn(Optional.of(member(childId, UserRole.CHILD)));
	}

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

	@Nested
	@DisplayName("경로의 가족·아이를 볼 때")
	class Validate {

		@Test
		@DisplayName("부모가 자기 가족 아이를 지목하면 통과한다")
		void 부모는_통과() {
			assertThatCode(() -> accessValidator.validate(PARENT_ID, FAMILY_ID, CHILD_ID))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("아이가 자기 자신을 지목하면 통과한다")
		void 아이는_자기_것만_통과() {
			assertThatCode(() -> accessValidator.validate(CHILD_ID, FAMILY_ID, CHILD_ID))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("없거나 지워진 가족이면 404다")
		void 없는_가족은_404() {
			given(familyRepository.findByIdAndDeletedAtIsNull(OUTSIDER_ID)).willReturn(Optional.empty());

			assertThatThrownBy(() -> accessValidator.validate(PARENT_ID, OUTSIDER_ID, CHILD_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAMILY_NOT_FOUND);
		}

		@Test
		@DisplayName("그 가족의 구성원이 아니면 403이 아니라 404다")
		void 남의_가족은_404() {
			givenUser(OUTSIDER_ID, UserRole.PARENT);
			given(familyMemberRepository.existsActiveMember(FAMILY_ID, OUTSIDER_ID)).willReturn(false);

			assertThatThrownBy(() -> accessValidator.validate(OUTSIDER_ID, FAMILY_ID, CHILD_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAMILY_NOT_FOUND);
		}

		@Test
		@DisplayName("경로의 childId 가 그 가족 자녀가 아니면 404다")
		void 남의_자녀는_404() {
			given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, OUTSIDER_ID))
				.willReturn(Optional.empty());

			assertThatThrownBy(() -> accessValidator.validate(PARENT_ID, FAMILY_ID, OUTSIDER_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
		}

		@Test
		@DisplayName("아이가 형제를 지목하면 403이다")
		void 아이는_형제_것을_볼_수_없다() {
			assertThatThrownBy(() -> accessValidator.validate(CHILD_ID, FAMILY_ID, SIBLING_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("아이만 할 수 있는 동작일 때")
	class RequireChild {

		@Test
		@DisplayName("아이는 통과한다")
		void 아이는_통과() {
			assertThatCode(() -> accessValidator.requireChild(CHILD_ID)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("부모는 403이다")
		void 부모는_403() {
			// 오늘의 퀴즈 조회·제출은 아이만 한다(QUIZ-02·03). validate 는 통과시키므로
			// 여기서 한 번 더 좁힌다.
			assertThatThrownBy(() -> accessValidator.requireChild(PARENT_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
		}

		@Test
		@DisplayName("없는 사용자면 401이다")
		void 없는_사용자는_401() {
			given(userRepository.findById(OUTSIDER_ID)).willReturn(Optional.empty());

			assertThatThrownBy(() -> accessValidator.requireChild(OUTSIDER_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
		}
	}
}
