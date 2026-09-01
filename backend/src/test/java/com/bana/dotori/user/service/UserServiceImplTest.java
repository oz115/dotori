package com.bana.dotori.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
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

	@Spy
	private UserMapper userMapper;

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
}
