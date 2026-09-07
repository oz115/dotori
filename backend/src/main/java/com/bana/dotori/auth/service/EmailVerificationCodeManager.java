package com.bana.dotori.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이메일 인증 코드·verificationToken 의 Redis 저장소 (AUTH-07).
 *
 * <p>DB 대신 Redis 를 쓰는 이유: 코드와 토큰 모두 TTL 이 지나면 그대로 사라져야 하는
 * 단명 데이터라 만료 배치가 필요 없고, 1원 인증 세션({@code AccountAuthSessionManager})과
 * 같은 방식을 유지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCodeManager {

	private static final String KEY_PREFIX = "auth:email-verification:";
	private static final String FIELD_CODE = ":code";
	private static final String FIELD_ATTEMPT = ":attempt";
	private static final String FIELD_COOLDOWN = ":cooldown";
	private static final long CODE_TTL_SECONDS = 600L;
	private static final long COOLDOWN_SECONDS = 60L;

	private final StringRedisTemplate redisTemplate;

	/**
	 * 인증 코드를 저장한다. 기존 코드가 있으면 덮어써서 무효화하고 시도 횟수도 초기화한다.
	 * 재발송 쿨타임(1분)이 지나지 않았으면 TOO_MANY_REQUESTS 예외를 발생시킨다.
	 *
	 * @param email 인증 코드를 받을 이메일 (소문자 정규화된 값)
	 * @param code  6자리 숫자 인증 코드
	 */
	public void saveCode(String email, String code) {
		String cooldownKey = buildKey(email) + FIELD_COOLDOWN;
		if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
			throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
		}

		Duration codeTtl = Duration.ofSeconds(CODE_TTL_SECONDS);
		redisTemplate.opsForValue().set(buildKey(email) + FIELD_CODE, code, codeTtl);
		redisTemplate.opsForValue().set(buildKey(email) + FIELD_ATTEMPT, "0", codeTtl);
		redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(COOLDOWN_SECONDS));
	}

	/**
	 * 코드의 유효 시간(초)을 반환한다.
	 *
	 * @return 코드 TTL(초)
	 */
	public long codeTtlSeconds() {
		return CODE_TTL_SECONDS;
	}

	private String buildKey(String email) {
		return KEY_PREFIX + email;
	}
}
