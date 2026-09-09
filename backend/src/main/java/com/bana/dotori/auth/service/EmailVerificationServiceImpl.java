package com.bana.dotori.auth.service;

import java.util.Locale;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.bana.dotori.auth.dto.EmailVerificationPurpose;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.util.RandomCodeGenerator;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

	private static final int CODE_LENGTH = 6;

	private final EmailVerificationCodeManager codeManager;
	private final JavaMailSender mailSender;
	private final UserRepository userRepository;

	@Override
	public void sendCode(String email, EmailVerificationPurpose purpose) {
		// 회원가입(UserServiceImpl.normalizeEmail)과 같은 기준으로 소문자 정규화한다.
		// 발송과 확인이 다른 대소문자로 들어와도 같은 Redis 키를 보게 하려는 것이다.
		String normalizedEmail = email.toLowerCase(Locale.ROOT);

		// 회원가입 용도만 가입 여부를 확인한다. 이미 가입된 이메일이면 메일도 쿨타임도 없이
		// 즉시 409 로 알려줘서, 마지막 가입하기 단계가 아니라 이메일 입력 시점에 걸러낸다.
		// 다른 용도(아이디 찾기·비밀번호 재설정)는 가입된 이메일이어야 하므로 검사하지 않고,
		// 계정 존재 여부도 노출하지 않는다.
		if (purpose == EmailVerificationPurpose.SIGNUP && userRepository.existsByEmail(normalizedEmail)) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}
		// 가족 초대 코드와 같은 문자셋(혼동 문자를 뺀 대문자·숫자)의 6자리 코드
		String code = RandomCodeGenerator.generate(CODE_LENGTH);

		// 쿨타임 검사를 겸하므로 저장이 먼저다. 저장이 거부되면 메일도 나가지 않는다.
		codeManager.saveCode(normalizedEmail, code);

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(normalizedEmail);
		message.setSubject("[도토리타운] 이메일 인증 코드");
		message.setText("인증 코드: " + code + "\n"
			+ (codeManager.codeTtlSeconds() / 60) + "분 안에 입력해 주세요.");
		try {
			mailSender.send(message);
		} catch (MailException e) {
			// 메일이 못 나갔는데 코드·쿨타임만 남으면 사용자는 받지도 못한 코드에
			// 60초간 재시도조차 막힌다 → 저장한 것을 되돌려 즉시 재시도할 수 있게 한다
			codeManager.discardCode(normalizedEmail);
			throw e;
		}
	}

	@Override
	public String confirmCode(String email, String code) {
		// 가족 초대 코드 입력(FamilyServiceImpl.connect)과 같은 정규화 — 소문자로 입력해도 통과시킨다
		return codeManager.verifyCode(email.toLowerCase(Locale.ROOT), code.strip().toUpperCase(Locale.ROOT));
	}
}
