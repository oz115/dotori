package com.bana.dotori.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChosungHintTest {

	@Test
	@DisplayName("한글은 초성으로 바꾸고 띄어쓰기는 그대로 둔다")
	void of_hangul() {
		assertThat(ChosungHint.of("물가가 계속 오르는 현상")).isEqualTo("ㅁㄱㄱ ㄱㅅ ㅇㄹㄴ ㅎㅅ");
	}

	@Test
	@DisplayName("쌍자음·받침 있는 글자도 초성만 남긴다")
	void of_doubleConsonantAndBatchim() {
		assertThat(ChosungHint.of("빠른 값어치")).isEqualTo("ㅃㄹ ㄱㅇㅊ");
		assertThat(ChosungHint.of("꽃")).isEqualTo("ㄲ");
	}

	@Test
	@DisplayName("영문·숫자는 정답이 새지 않도록 가리고 문장부호는 그대로 둔다")
	void of_masksLettersAndDigits() {
		assertThat(ChosungHint.of("GDP")).isEqualTo("○○○");
		assertThat(ChosungHint.of("3배 (이자)")).isEqualTo("○ㅂ (ㅇㅈ)");
	}

	@Test
	@DisplayName("앞뒤 공백은 잘라내고 null은 빈 문자열로 처리한다")
	void of_trimAndNull() {
		assertThat(ChosungHint.of("  이자  ")).isEqualTo("ㅇㅈ");
		assertThat(ChosungHint.of(null)).isEmpty();
	}
}
