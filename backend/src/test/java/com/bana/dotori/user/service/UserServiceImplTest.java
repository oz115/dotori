package com.bana.dotori.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.auth.service.AuthService;
import com.bana.dotori.family.dto.ConnectedChild;
import com.bana.dotori.family.service.FamilyService;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.dto.ConnectedChildrenResponse;
import com.bana.dotori.user.dto.PasswordChangeRequest;
import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
import com.bana.dotori.user.dto.UserInfoResponse;
import com.bana.dotori.user.dto.UserUpdateRequest;
import com.bana.dotori.user.dto.UserUpdateResponse;
import com.bana.dotori.user.dto.UserWithdrawRequest;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.mapper.UserMapper;
import com.bana.dotori.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthService authService;

	@Mock
	private FamilyService familyService;

	@Mock
	private RemainingAssetReader remainingAssetReader;

	@Spy
	private UserMapper userMapper;

	@Spy
	private MyPageAccessPolicy accessPolicy;

	@InjectMocks
	private UserServiceImpl userService;

	private SignupRequest signupRequest() {
		return new SignupRequest("dotori123", "P@ssw0rd!", "김도토", "dotori@example.com", UserRole.PARENT);
	}

	@Test
	@DisplayName("회원가입 성공 시 저장된 유저 정보를 반환한다")
	void signup_success() {
		SignupRequest request = signupRequest();
		given(userRepository.existsByLoginId("dotori123")).willReturn(false);
		given(userRepository.existsByEmail("dotori@example.com")).willReturn(false);
		given(passwordEncoder.encode("P@ssw0rd!")).willReturn("encoded-password");
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = userService.signup(request);

		assertThat(response.loginId()).isEqualTo("dotori123");
		assertThat(response.name()).isEqualTo("김도토");
		assertThat(response.email()).isEqualTo("dotori@example.com");
		assertThat(response.role()).isEqualTo(UserRole.PARENT);
	}

	@Test
	@DisplayName("이메일은 소문자로 정규화되어 중복 체크·저장된다")
	void signup_normalizesEmail() {
		SignupRequest request = new SignupRequest("dotori123", "P@ssw0rd!", "김도토", "Dotori@Example.COM",
			UserRole.PARENT);
		given(userRepository.existsByLoginId("dotori123")).willReturn(false);
		given(userRepository.existsByEmail("dotori@example.com")).willReturn(false);
		given(passwordEncoder.encode("P@ssw0rd!")).willReturn("encoded-password");
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = userService.signup(request);

		assertThat(response.email()).isEqualTo("dotori@example.com");
		verify(userRepository).existsByEmail("dotori@example.com");
		verify(userRepository).save(argThat(user -> "dotori@example.com".equals(user.getEmail())));
	}

	@Test
	@DisplayName("이미 사용 중인 아이디면 DUPLICATE_LOGIN_ID 예외가 발생한다")
	void signup_duplicateLoginId() {
		given(userRepository.existsByLoginId("dotori123")).willReturn(true);

		assertThatThrownBy(() -> userService.signup(signupRequest()))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
	}

	@Test
	@DisplayName("이미 사용 중인 이메일이면 DUPLICATE_EMAIL 예외가 발생한다")
	void signup_duplicateEmail() {
		given(userRepository.existsByLoginId("dotori123")).willReturn(false);
		given(userRepository.existsByEmail("dotori@example.com")).willReturn(true);

		assertThatThrownBy(() -> userService.signup(signupRequest()))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.DUPLICATE_EMAIL);
	}

	@Test
	@DisplayName("동시 요청으로 저장 시점에 unique 제약 위반이 나면 409로 변환된다")
	void signup_raceConditionOnLoginId() {
		given(userRepository.existsByLoginId("dotori123")).willReturn(false);
		given(userRepository.existsByEmail("dotori@example.com")).willReturn(false);
		given(passwordEncoder.encode("P@ssw0rd!")).willReturn("encoded-password");
		given(userRepository.save(any(User.class))).willThrow(new DataIntegrityViolationException(
			"could not execute statement",
			new RuntimeException("ERROR: duplicate key value violates unique constraint \"uk_users_login_id\"")));

		assertThatThrownBy(() -> userService.signup(signupRequest()))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
	}

	@Test
	@DisplayName("동시 요청으로 이메일 unique 제약 위반이 나면 DUPLICATE_EMAIL로 변환된다")
	void signup_raceConditionOnEmail() {
		given(userRepository.existsByLoginId("dotori123")).willReturn(false);
		given(userRepository.existsByEmail("dotori@example.com")).willReturn(false);
		given(passwordEncoder.encode("P@ssw0rd!")).willReturn("encoded-password");
		given(userRepository.save(any(User.class))).willThrow(new DataIntegrityViolationException(
			"could not execute statement",
			new RuntimeException("ERROR: duplicate key value violates unique constraint \"uk_users_email\"")));

		assertThatThrownBy(() -> userService.signup(signupRequest()))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.DUPLICATE_EMAIL);
	}

	@Test
	@DisplayName("비밀번호는 인코딩되어 저장된다")
	void signup_encodesPassword() {
		SignupRequest request = signupRequest();
		given(userRepository.existsByLoginId(anyString())).willReturn(false);
		given(userRepository.existsByEmail(anyString())).willReturn(false);
		given(passwordEncoder.encode("P@ssw0rd!")).willReturn("encoded-password");
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		userService.signup(request);

		verify(userRepository).save(argThat(user -> "encoded-password".equals(user.getPassword())));
	}

	@Test
	@DisplayName("본인 계정을 조회하면 명세의 회원 정보를 돌려준다")
	void getUserInfo_success() {
		given(userRepository.findById(1)).willReturn(Optional.of(user(1)));

		UserInfoResponse response = userService.getUserInfo(1, 1);

		assertThat(response.userId()).isEqualTo(1);
		assertThat(response.loginId()).isEqualTo("dotori123");
		assertThat(response.name()).isEqualTo("김도토");
		assertThat(response.email()).isEqualTo("dotori@example.com");
		assertThat(response.role()).isEqualTo(UserRole.PARENT);
		assertThat(response.emailVerified()).isFalse();
		assertThat(response.createdAt()).isNotNull();
	}

	@Test
	@DisplayName("남의 계정을 조회하면 조회 자체를 하지 않고 FORBIDDEN 예외가 발생한다")
	void getUserInfo_otherUserIsForbidden() {
		assertThatThrownBy(() -> userService.getUserInfo(1, 2))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);

		// 존재 여부를 확인하기 전에 막아야 응답 코드만으로 다른 계정의 존재를 알아낼 수 없다.
		verify(userRepository, never()).findById(any());
	}

	@Test
	@DisplayName("본인 계정인데 회원이 없으면 NOT_FOUND 예외가 발생한다")
	void getUserInfo_notFound() {
		given(userRepository.findById(1)).willReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getUserInfo(1, 1))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.NOT_FOUND);
	}

	private User user(Integer id) {
		User user = User.builder()
			.loginId("dotori123")
			.password("encoded-password")
			.name("김도토")
			.email("dotori@example.com")
			.role(UserRole.PARENT)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "createdAt",
			OffsetDateTime.of(2026, 9, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
		ReflectionTestUtils.setField(user, "updatedAt",
			OffsetDateTime.of(2026, 9, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
		return user;
	}

	@Test
	@DisplayName("이름을 수정하면 바뀐 이름과 수정 일시를 돌려준다")
	void updateUserInfo_success() {
		User stored = user(1);
		given(userRepository.findById(1)).willReturn(Optional.of(stored));
		given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		UserUpdateResponse response = userService.updateUserInfo(1, 1, new UserUpdateRequest("김도토리"));

		assertThat(response.userId()).isEqualTo(1);
		assertThat(response.name()).isEqualTo("김도토리");
		assertThat(stored.getName()).isEqualTo("김도토리");
		// updatedAt 은 @UpdateTimestamp 가 flush 때 채우므로 여기서는 매핑만 확인한다.
		// 실제로 값이 갱신되는지는 DB 를 띄우는 통합 테스트(S15P21A505-73)에서 본다.
		assertThat(response.updatedAt()).isEqualTo(stored.getUpdatedAt());
		verify(userRepository).saveAndFlush(stored);
	}

	@Test
	@DisplayName("전달하지 않은 필드는 기존 값이 유지된다")
	void updateUserInfo_keepsUnsentFields() {
		User stored = user(1);
		given(userRepository.findById(1)).willReturn(Optional.of(stored));
		given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		UserUpdateResponse response = userService.updateUserInfo(1, 1, new UserUpdateRequest(null));

		assertThat(response.name()).isEqualTo("김도토");
		assertThat(stored.getEmail()).isEqualTo("dotori@example.com");
		assertThat(stored.getRole()).isEqualTo(UserRole.PARENT);
	}

	@Test
	@DisplayName("남의 계정을 수정하면 조회 자체를 하지 않고 FORBIDDEN 예외가 발생한다")
	void updateUserInfo_otherUserIsForbidden() {
		assertThatThrownBy(() -> userService.updateUserInfo(1, 2, new UserUpdateRequest("김도토리")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);

		verify(userRepository, never()).findById(any());
		verify(userRepository, never()).saveAndFlush(any(User.class));
	}

	@Test
	@DisplayName("본인 계정인데 회원이 없으면 NOT_FOUND 예외가 발생한다")
	void updateUserInfo_notFound() {
		given(userRepository.findById(1)).willReturn(Optional.empty());

		assertThatThrownBy(() -> userService.updateUserInfo(1, 1, new UserUpdateRequest("김도토리")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("비밀번호를 바꾸면 인코딩해 저장하고 기존 Refresh Token 을 모두 폐기한다")
	void changePassword_success() {
		User stored = user(1);
		given(userRepository.findById(1)).willReturn(Optional.of(stored));
		given(passwordEncoder.matches("P@ssw0rd!", "encoded-password")).willReturn(true);
		given(passwordEncoder.encode("NewP@ssw0rd!")).willReturn("encoded-new-password");

		userService.changePassword(1, 1, new PasswordChangeRequest("P@ssw0rd!", "NewP@ssw0rd!"));

		assertThat(stored.getPassword()).isEqualTo("encoded-new-password");
		verify(authService).revokeAllRefreshTokens(1);
	}

	@Test
	@DisplayName("현재 비밀번호가 틀리면 PASSWORD_MISMATCH 예외가 나고 토큰도 유지된다")
	void changePassword_currentPasswordMismatch() {
		User stored = user(1);
		given(userRepository.findById(1)).willReturn(Optional.of(stored));
		given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

		assertThatThrownBy(() -> userService.changePassword(1, 1,
			new PasswordChangeRequest("wrong-password", "NewP@ssw0rd!")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.PASSWORD_MISMATCH);

		assertThat(stored.getPassword()).isEqualTo("encoded-password");
		verify(authService, never()).revokeAllRefreshTokens(any());
	}

	@Test
	@DisplayName("새 비밀번호가 8자 미만이면 INVALID_PASSWORD_FORMAT 예외가 발생한다")
	void changePassword_tooShortNewPassword() {
		given(userRepository.findById(1)).willReturn(Optional.of(user(1)));
		given(passwordEncoder.matches("P@ssw0rd!", "encoded-password")).willReturn(true);

		assertThatThrownBy(() -> userService.changePassword(1, 1,
			new PasswordChangeRequest("P@ssw0rd!", "short")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_PASSWORD_FORMAT);

		verify(authService, never()).revokeAllRefreshTokens(any());
	}

	@Test
	@DisplayName("새 비밀번호가 공백뿐이면 길이를 채워도 INVALID_PASSWORD_FORMAT 예외가 발생한다")
	void changePassword_blankNewPassword() {
		given(userRepository.findById(1)).willReturn(Optional.of(user(1)));
		given(passwordEncoder.matches("P@ssw0rd!", "encoded-password")).willReturn(true);

		assertThatThrownBy(() -> userService.changePassword(1, 1,
			new PasswordChangeRequest("P@ssw0rd!", "          ")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_PASSWORD_FORMAT);
	}

	@Test
	@DisplayName("남의 비밀번호를 바꾸려 하면 조회 자체를 하지 않고 FORBIDDEN 예외가 발생한다")
	void changePassword_otherUserIsForbidden() {
		assertThatThrownBy(() -> userService.changePassword(1, 2,
			new PasswordChangeRequest("P@ssw0rd!", "NewP@ssw0rd!")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);

		verify(userRepository, never()).findById(any());
		verify(authService, never()).revokeAllRefreshTokens(any());
	}

	@Test
	@DisplayName("본인 계정인데 회원이 없으면 NOT_FOUND 예외가 발생한다")
	void changePassword_notFound() {
		given(userRepository.findById(1)).willReturn(Optional.empty());

		assertThatThrownBy(() -> userService.changePassword(1, 1,
			new PasswordChangeRequest("P@ssw0rd!", "NewP@ssw0rd!")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("부모가 조회하면 연결된 자녀 목록을 돌려준다")
	void getConnectedChildren_success() {
		given(userRepository.findById(1)).willReturn(Optional.of(user(1)));
		given(familyService.findConnectedChildren(1)).willReturn(List.of(
			new ConnectedChild(2, "김아이", OffsetDateTime.of(2026, 8, 28, 10, 0, 0, 0, ZoneOffset.ofHours(9)))));

		ConnectedChildrenResponse response = userService.getConnectedChildren(1, 1);

		assertThat(response.children()).hasSize(1);
		assertThat(response.children().get(0).userId()).isEqualTo(2);
		assertThat(response.children().get(0).name()).isEqualTo("김아이");
		assertThat(response.children().get(0).joinedAt()).isNotNull();
	}

	@Test
	@DisplayName("연결된 자녀가 없으면 오류 대신 빈 목록을 돌려준다")
	void getConnectedChildren_emptyList() {
		given(userRepository.findById(1)).willReturn(Optional.of(user(1)));
		given(familyService.findConnectedChildren(1)).willReturn(List.of());

		ConnectedChildrenResponse response = userService.getConnectedChildren(1, 1);

		assertThat(response.children()).isEmpty();
	}

	@Test
	@DisplayName("자녀 계정이 조회하면 FORBIDDEN 예외가 발생한다")
	void getConnectedChildren_childIsForbidden() {
		User child = user(1);
		ReflectionTestUtils.setField(child, "role", UserRole.CHILD);
		given(userRepository.findById(1)).willReturn(Optional.of(child));

		assertThatThrownBy(() -> userService.getConnectedChildren(1, 1))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);

		verify(familyService, never()).findConnectedChildren(any());
	}

	@Test
	@DisplayName("남의 자녀 목록을 조회하면 조회 자체를 하지 않고 FORBIDDEN 예외가 발생한다")
	void getConnectedChildren_otherUserIsForbidden() {
		assertThatThrownBy(() -> userService.getConnectedChildren(1, 2))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);

		verify(userRepository, never()).findById(any());
		verify(familyService, never()).findConnectedChildren(any());
	}

	@Test
	@DisplayName("탈퇴하면 계정을 비활성으로 표시하고 Refresh Token 을 모두 폐기한다")
	void withdraw_success() {
		User stored = user(1);
		given(userRepository.findById(1)).willReturn(Optional.of(stored));
		given(passwordEncoder.matches("P@ssw0rd!", "encoded-password")).willReturn(true);
		given(remainingAssetReader.hasRemainingAsset(1)).willReturn(false);

		userService.withdraw(1, 1, new UserWithdrawRequest("P@ssw0rd!"));

		assertThat(stored.getDeletedAt()).isNotNull();
		verify(authService).revokeAllRefreshTokens(1);
	}

	@Test
	@DisplayName("잔여 자산이 있으면 HAS_ACTIVE_ASSETS 예외가 나고 계정은 그대로다")
	void withdraw_hasRemainingAsset() {
		User stored = user(1);
		given(userRepository.findById(1)).willReturn(Optional.of(stored));
		given(passwordEncoder.matches("P@ssw0rd!", "encoded-password")).willReturn(true);
		given(remainingAssetReader.hasRemainingAsset(1)).willReturn(true);

		assertThatThrownBy(() -> userService.withdraw(1, 1, new UserWithdrawRequest("P@ssw0rd!")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.HAS_ACTIVE_ASSETS);

		assertThat(stored.getDeletedAt()).isNull();
		verify(authService, never()).revokeAllRefreshTokens(any());
	}

	@Test
	@DisplayName("비밀번호가 틀리면 잔여 자산을 보기도 전에 PASSWORD_MISMATCH 예외가 발생한다")
	void withdraw_passwordMismatch() {
		User stored = user(1);
		given(userRepository.findById(1)).willReturn(Optional.of(stored));
		given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

		assertThatThrownBy(() -> userService.withdraw(1, 1, new UserWithdrawRequest("wrong-password")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.PASSWORD_MISMATCH);

		assertThat(stored.getDeletedAt()).isNull();
		verify(remainingAssetReader, never()).hasRemainingAsset(any());
	}

	@Test
	@DisplayName("이미 탈퇴한 계정은 없는 것으로 보고 NOT_FOUND 예외가 발생한다")
	void withdraw_alreadyWithdrawn() {
		User stored = user(1);
		stored.withdraw();
		given(userRepository.findById(1)).willReturn(Optional.of(stored));

		assertThatThrownBy(() -> userService.withdraw(1, 1, new UserWithdrawRequest("P@ssw0rd!")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("남의 계정을 탈퇴시키려 하면 조회 자체를 하지 않고 FORBIDDEN 예외가 발생한다")
	void withdraw_otherUserIsForbidden() {
		assertThatThrownBy(() -> userService.withdraw(1, 2, new UserWithdrawRequest("P@ssw0rd!")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);

		verify(userRepository, never()).findById(any());
	}
}
