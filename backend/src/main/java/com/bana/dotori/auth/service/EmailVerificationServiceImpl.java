package com.bana.dotori.auth.service;

import java.security.SecureRandom;
import java.util.Locale;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final EmailVerificationCodeManager codeManager;
	private final JavaMailSender mailSender;

	@Override
	public void sendCode(String email) {
		// 회원가입(UserServiceImpl.normalizeEmail)과 같은 기준으로 소문자 정규화한다.
		// 발송과 확인이 다른 대소문자로 들어와도 같은 Redis 키를 보게 하려는 것이다.
		String normalizedEmail = email.toLowerCase(Locale.ROOT);
		String code = String.format("%06d", RANDOM.nextInt(1_000_000));

		// 쿨타임 검사를 겸하므로 저장이 먼저다. 저장이 거부되면 메일도 나가지 않는다.
		codeManager.saveCode(normalizedEmail, code);

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(normalizedEmail);
		message.setSubject("[도토리타운] 이메일 인증 코드");
		message.setText("인증 코드: " + code + "\n"
			+ (codeManager.codeTtlSeconds() / 60) + "분 안에 입력해 주세요.");
		mailSender.send(message);
	}

	@Override
	public String confirmCode(String email, String code) {
		return codeManager.verifyCode(email.toLowerCase(Locale.ROOT), code);
	}
}
