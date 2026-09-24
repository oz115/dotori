package com.bana.dotori.global.util;

/**
 * 정답 문자열을 초성 힌트로 바꾼다 (예: "물가가 오르는 현상" → "ㅁㄱㄱ ㅇㄹㄴ ㅎㅅ").
 *
 * <p>글자 수와 띄어쓰기는 그대로 보여 줘서 "몇 글자, 몇 단어"인지 짐작할 수 있게 한다.
 * 한글이 아닌 글자·숫자("GDP", "3배")는 그대로 두면 정답이 통째로 새므로 {@code ○}로 가리고,
 * 공백·문장부호만 그대로 둔다.
 */
public final class ChosungHint {

	private static final char[] CHOSUNG = {
		'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
		'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
	};

	/** 완성형 한글 음절의 범위 (가 ~ 힣). */
	private static final int HANGUL_FIRST = 0xAC00;
	private static final int HANGUL_LAST = 0xD7A3;

	/** 초성 하나가 묶는 음절 수 = 중성 21 × 종성 28. */
	private static final int SYLLABLES_PER_CHOSUNG = 21 * 28;

	private static final char MASK = '○';

	private ChosungHint() {
	}

	public static String of(String answer) {
		if (answer == null) {
			return "";
		}

		StringBuilder builder = new StringBuilder(answer.length());
		for (char c : answer.trim().toCharArray()) {
			if (c >= HANGUL_FIRST && c <= HANGUL_LAST) {
				builder.append(CHOSUNG[(c - HANGUL_FIRST) / SYLLABLES_PER_CHOSUNG]);
			} else if (Character.isLetterOrDigit(c)) {
				builder.append(MASK);
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
	}
}
