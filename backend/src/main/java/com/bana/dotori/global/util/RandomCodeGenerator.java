package com.bana.dotori.global.util;

import java.security.SecureRandom;

/**
 * 사용자가 보고 옮겨 적는 짧은 코드(가족 초대 코드, 이메일 인증 코드)의 공용 생성기.
 *
 * <p>혼동되는 문자(0, O, 1, I)를 뺀 대문자·숫자 32자만 쓴다.
 * 두 코드가 같은 문자셋을 쓰므로 입력 UI·정규화 규칙(대문자 변환)도 같이 갈 수 있다.
 */
public final class RandomCodeGenerator {

	private static final String CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private RandomCodeGenerator() {
	}

	public static String generate(int length) {
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			builder.append(CODE_CHARACTERS.charAt(SECURE_RANDOM.nextInt(CODE_CHARACTERS.length())));
		}
		return builder.toString();
	}
}
