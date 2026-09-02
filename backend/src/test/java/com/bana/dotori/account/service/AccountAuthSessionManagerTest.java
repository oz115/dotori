package com.bana.dotori.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class AccountAuthSessionManagerTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOps;

	@InjectMocks
	private AccountAuthSessionManager sessionManager;

	private static final Integer USER_ID = 1;
	private static final String ACCOUNT_NO = "0011214764051239";
	private static final String USER_KEY = "633d65a0-67c4-48b2-9bbf-94b948d8e141";

	@Test
	@DisplayName("create_호출시_userKey와_attempt가_Redis에_TTL과_함께_저장된다")
	void create_정상저장() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOps);

		// when
		sessionManager.create(USER_ID, ACCOUNT_NO, USER_KEY);

		// then
		verify(valueOps, times(2)).set(any(String.class), any(String.class), any(Duration.class));
	}

	@Test
	@DisplayName("getUserKey_세션이_존재하면_userKey를_반환한다")
	void getUserKey_세션존재_반환성공() {
		// given
		String expectedKey = "account:auth:1:0011214764051239:userKey";
		given(redisTemplate.opsForValue()).willReturn(valueOps);
		given(valueOps.get(expectedKey)).willReturn(USER_KEY);

		// when
		String result = sessionManager.getUserKey(USER_ID, ACCOUNT_NO);

		// then
		assertThat(result).isEqualTo(USER_KEY);
	}

	@Test
	@DisplayName("getUserKey_세션이_없으면_AUTH_SESSION_NOT_FOUND_예외가_발생한다")
	void getUserKey_세션없음_예외발생() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOps);
		given(valueOps.get(any())).willReturn(null);

		// when & then
		assertThatThrownBy(() -> sessionManager.getUserKey(USER_ID, ACCOUNT_NO))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_SESSION_NOT_FOUND));
	}

	@Test
	@DisplayName("incrementAttempt_MAX_ATTEMPT_미만이면_카운트만_증가한다")
	void incrementAttempt_횟수미달_카운트증가() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOps);
		given(valueOps.increment(any())).willReturn(3L);

		// when & then (예외 없이 통과)
		sessionManager.incrementAttempt(USER_ID, ACCOUNT_NO);

		verify(valueOps).increment(any());
	}

	@Test
	@DisplayName("incrementAttempt_MAX_ATTEMPT_도달시_세션_삭제_후_AUTH_ATTEMPT_EXCEEDED_예외가_발생한다")
	void incrementAttempt_횟수초과_예외발생() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOps);
		given(valueOps.increment(any())).willReturn(5L);

		// when & then
		assertThatThrownBy(() -> sessionManager.incrementAttempt(USER_ID, ACCOUNT_NO))
			.isInstanceOf(BusinessException.class)
			.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_ATTEMPT_EXCEEDED));

		verify(redisTemplate, times(2)).delete(any(String.class));
	}

	@Test
	@DisplayName("delete_호출시_userKey와_attempt_키가_모두_삭제된다")
	void delete_세션삭제() {
		// when
		sessionManager.delete(USER_ID, ACCOUNT_NO);

		// then
		verify(redisTemplate, times(2)).delete(any(String.class));
	}

	@Test
	@DisplayName("exists_세션이_존재하면_true를_반환한다")
	void exists_세션존재_true반환() {
		// given
		given(redisTemplate.hasKey(any())).willReturn(Boolean.TRUE);

		// when
		boolean result = sessionManager.exists(USER_ID, ACCOUNT_NO);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("exists_세션이_없으면_false를_반환한다")
	void exists_세션없음_false반환() {
		// given
		given(redisTemplate.hasKey(any())).willReturn(null);

		// when
		boolean result = sessionManager.exists(USER_ID, ACCOUNT_NO);

		// then
		assertThat(result).isFalse();
	}
}
