package com.bana.dotori.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;

/**
 * 마이페이지 접근 판정 자체만 검증한다. 여기가 실패하면 원인은 개별 API 가 아니라 판정 규칙이다.
 */
class MyPageAccessPolicyTest {

	private final MyPageAccessPolicy accessPolicy = new MyPageAccessPolicy();

	@Test
	@DisplayName("본인 요청은 통과한다")
	void requireSelf_sameUserPasses() {
		assertThatCode(() -> accessPolicy.requireSelf(1, 1)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("남의 리소스 요청은 FORBIDDEN 으로 막는다")
	void requireSelf_otherUserIsForbidden() {
		assertThatThrownBy(() -> accessPolicy.requireSelf(1, 2))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("인증 주체가 없으면 403 이 아니라 UNAUTHORIZED 로 막는다")
	void requireSelf_nullPrincipalIsUnauthorized() {
		assertThatThrownBy(() -> accessPolicy.requireSelf(null, 1))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("부모 계정은 통과한다")
	void requireParent_parentPasses() {
		assertThatCode(() -> accessPolicy.requireParent(user(UserRole.PARENT))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("자녀 계정은 FORBIDDEN 으로 막는다")
	void requireParent_childIsForbidden() {
		assertThatThrownBy(() -> accessPolicy.requireParent(user(UserRole.CHILD)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	private User user(UserRole role) {
		User user = User.builder()
			.loginId("dotori123")
			.password("encoded-password")
			.name("김도토")
			.email("dotori@example.com")
			.role(role)
			.build();
		ReflectionTestUtils.setField(user, "id", 1);
		return user;
	}
}
