package com.bana.dotori.chore.service;

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
 * 2계층 - 집안일 API 경로의 인가 규칙 (CHORE 전역).
 *
 * <p>이 규칙의 <b>정본 테스트</b>다. 집안일 서비스 6개가 전부 이 검증기를 거치므로,
 * 규칙이 맞는지는 여기서만 확인하고 각 서비스 테스트는 "검증기를 거치는가"만 본다.
 * 예전에는 같은 규칙이 {@code ChoreServiceImpl} 안에도 private 으로 복제돼 있었고,
 * 그래서 한쪽만 고치면 나머지 절반이 옛 규칙으로 도는 상태였다.
 *
 * <p>상태 코드가 곧 정보 노출 정책이다. 404 와 403 을 바꾸면 남의 가족·형제의 존재가
 * 새어 나가므로, 어느 경우에 무엇이 나가는지를 값으로 못박아 둔다.
 */
@DisplayName("ChoreAccessValidator 집안일 인가 규칙")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChoreAccessValidatorTest {

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
	private ChoreAccessValidator accessValidator;

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

			// 403 을 주면 그 familyId 가 실재한다는 사실이 새어 나간다.
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
		@DisplayName("경로의 childId 가 부모면 404다")
		void 부모를_childId로_넣으면_404() {
			given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, PARENT_ID))
				.willReturn(Optional.of(member(PARENT_ID, UserRole.PARENT)));

			// 집안일은 아이에게 배정되는 것이라 부모를 대상으로 지목할 수 없다.
			assertThatThrownBy(() -> accessValidator.validate(PARENT_ID, FAMILY_ID, PARENT_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
		}

		@Test
		@DisplayName("아이가 형제를 지목하면 403이다")
		void 아이는_형제_것을_볼_수_없다() {
			// 형제의 존재는 이미 아는 사실이라 숨길 것이 없고, 권한 문제임을 알려 주는 편이 낫다.
			assertThatThrownBy(() -> accessValidator.validate(CHILD_ID, FAMILY_ID, SIBLING_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
		}

		@Test
		@DisplayName("가족을 먼저 보고 아이를 나중에 본다")
		void 가족을_먼저_본다() {
			// 없는 가족에 없는 아이를 함께 넣었을 때 가족 쪽 404 가 나가야 한다.
			// 순서가 뒤집히면 그 가족이 없다는 사실보다 아이 정보가 먼저 새어 나간다.
			given(familyRepository.findByIdAndDeletedAtIsNull(OUTSIDER_ID)).willReturn(Optional.empty());

			assertThatThrownBy(() -> accessValidator.validate(PARENT_ID, OUTSIDER_ID, OUTSIDER_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAMILY_NOT_FOUND);
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
			// validate 는 "아이라면 자기 것인가"만 보므로 부모도 통과한다. 완료 인증을
			// 올리는 것은 아이라서 여기서 한 번 더 좁힌다 (CHORE-11·12·13).
			assertThatThrownBy(() -> accessValidator.requireChild(PARENT_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("부모만 할 수 있는 동작일 때")
	class RequireParent {

		@Test
		@DisplayName("부모는 통과한다")
		void 부모는_통과() {
			assertThatCode(() -> accessValidator.requireParent(PARENT_ID)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("아이는 403이다")
		void 아이는_403() {
			// validate 만으로는 아이가 자기 대기함을 들여다볼 수 있다 (CHORE-08·09·10).
			assertThatThrownBy(() -> accessValidator.requireParent(CHILD_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("호출자를 찾을 때")
	class FindUser {

		@Test
		@DisplayName("없는 사용자면 401이다")
		void 없는_사용자는_401() {
			given(userRepository.findById(OUTSIDER_ID)).willReturn(Optional.empty());

			// 토큰은 유효했지만 그 사이 회원이 지워진 경우다. 404 가 아니라 401 로 준다.
			assertThatThrownBy(() -> accessValidator.findUser(OUTSIDER_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
		}

		@Test
		@DisplayName("있으면 그 사용자를 돌려준다")
		void 있으면_돌려준다() {
			assertThatCode(() -> accessValidator.findUser(PARENT_ID)).doesNotThrowAnyException();
		}
	}
}
