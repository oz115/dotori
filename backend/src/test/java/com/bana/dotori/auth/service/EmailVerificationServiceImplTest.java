package com.bana.dotori.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.bana.dotori.auth.dto.EmailVerificationPurpose;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

	@Mock
	private EmailVerificationCodeManager codeManager;

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private EmailVerificationServiceImpl emailVerificationService;

	private static final String EMAIL = "dotori@example.com";

	@Test
	@DisplayName("SIGNUP 용도에서 미가입 이메일이면 코드를 저장하고 메일을 발송한다")
	void sendCode_signupWithNewEmail() {
		given(userRepository.existsByEmailAndDeletedAtIsNull(EMAIL)).willReturn(false);

		emailVerificationService.sendCode(EMAIL, EmailVerificationPurpose.SIGNUP);

		verify(codeManager).saveCode(eq(EMAIL), anyString());
		verify(mailSender).send(any(SimpleMailMessage.class));
	}

	@Test
	@DisplayName("SIGNUP 용도에서 이미 가입된 이메일이면 DUPLICATE_EMAIL 예외가 발생하고 코드도 메일도 나가지 않는다")
	void sendCode_signupWithRegisteredEmail() {
		given(userRepository.existsByEmailAndDeletedAtIsNull(EMAIL)).willReturn(true);

		assertThatThrownBy(() -> emailVerificationService.sendCode(EMAIL, EmailVerificationPurpose.SIGNUP))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.DUPLICATE_EMAIL);

		then(codeManager).shouldHaveNoInteractions();
		then(mailSender).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("SIGNUP 중복 검사는 소문자로 정규화한 이메일로 한다 (대소문자 우회 차단)")
	void sendCode_signupNormalizesEmailBeforeDuplicateCheck() {
		given(userRepository.existsByEmailAndDeletedAtIsNull(EMAIL)).willReturn(true);

		assertThatThrownBy(() -> emailVerificationService.sendCode("Dotori@Example.COM",
			EmailVerificationPurpose.SIGNUP))
			.isInstanceOf(BusinessException.class);

		verify(userRepository).existsByEmailAndDeletedAtIsNull(EMAIL);
	}

	@Test
	@DisplayName("용도가 없으면 가입 여부를 확인하지 않고 발송한다 (계정 존재 여부 비노출 — 기존 동작)")
	void sendCode_withoutPurposeSkipsDuplicateCheck() {
		emailVerificationService.sendCode(EMAIL, null);

		then(userRepository).shouldHaveNoInteractions();
		verify(codeManager).saveCode(eq(EMAIL), anyString());
		verify(mailSender).send(any(SimpleMailMessage.class));
	}

	@Test
	@DisplayName("아이디 찾기 용도는 가입된 이메일이어도 그대로 발송한다")
	void sendCode_findIdIgnoresRegistration() {
		emailVerificationService.sendCode(EMAIL, EmailVerificationPurpose.FIND_ID);

		then(userRepository).shouldHaveNoInteractions();
		verify(codeManager).saveCode(eq(EMAIL), anyString());
		verify(mailSender).send(any(SimpleMailMessage.class));
	}
}
