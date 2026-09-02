package com.bana.dotori.account.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountAuthSessionManager {

	private static final String KEY_PREFIX = "account:auth:";
	private static final String FIELD_USER_KEY = ":userKey";
	private static final String FIELD_ATTEMPT = ":attempt";
	private static final long SESSION_TTL_SECONDS = 180L;
	private static final int MAX_ATTEMPT = 5;

	private final StringRedisTemplate redisTemplate;

	/**
	 * 1원 인증 세션을 생성한다.
	 * 기존 세션이 있으면 덮어쓰고 TTL을 초기화한다.
	 *
	 * @param userId    도토리 회원 ID
	 * @param accountNo 계좌번호
	 * @param userKey   금융망 사용자 키
	 */
	public void create(Integer userId, String accountNo, String userKey) {
		String userKeyRedisKey = buildKey(userId, accountNo) + FIELD_USER_KEY;
		String attemptRedisKey = buildKey(userId, accountNo) + FIELD_ATTEMPT;
		Duration ttl = Duration.ofSeconds(SESSION_TTL_SECONDS);

		redisTemplate.opsForValue().set(userKeyRedisKey, userKey, ttl);
		redisTemplate.opsForValue().set(attemptRedisKey, "0", ttl);
	}

	/**
	 * 세션에서 금융망 userKey를 조회한다.
	 * 세션이 없거나 만료된 경우 AUTH_SESSION_NOT_FOUND 예외를 발생시킨다.
	 *
	 * @param userId    도토리 회원 ID
	 * @param accountNo 계좌번호
	 * @return 금융망 사용자 키
	 */
	public String getUserKey(Integer userId, String accountNo) {
		String value = redisTemplate.opsForValue().get(buildKey(userId, accountNo) + FIELD_USER_KEY);
		if (value == null) {
			throw new BusinessException(ErrorCode.AUTH_SESSION_NOT_FOUND);
		}
		return value;
	}

	/**
	 * 인증 실패 시 시도 횟수를 증가시킨다.
	 * MAX_ATTEMPT 초과 시 세션을 삭제하고 AUTH_ATTEMPT_EXCEEDED 예외를 발생시킨다.
	 *
	 * @param userId    도토리 회원 ID
	 * @param accountNo 계좌번호
	 */
	public void incrementAttempt(Integer userId, String accountNo) {
		String attemptKey = buildKey(userId, accountNo) + FIELD_ATTEMPT;
		Long current = redisTemplate.opsForValue().increment(attemptKey);

		if (current == null || current >= MAX_ATTEMPT) {
			log.warn("1원 인증 횟수 초과 - userId: {}, accountNo: {}", userId, accountNo);
			delete(userId, accountNo);
			throw new BusinessException(ErrorCode.AUTH_ATTEMPT_EXCEEDED);
		}
	}

	/**
	 * 세션을 삭제한다 (인증 성공 또는 초과 시 호출).
	 *
	 * @param userId    도토리 회원 ID
	 * @param accountNo 계좌번호
	 */
	public void delete(Integer userId, String accountNo) {
		redisTemplate.delete(buildKey(userId, accountNo) + FIELD_USER_KEY);
		redisTemplate.delete(buildKey(userId, accountNo) + FIELD_ATTEMPT);
	}

	/**
	 * 세션이 존재하는지 확인한다.
	 *
	 * @param userId    도토리 회원 ID
	 * @param accountNo 계좌번호
	 * @return 세션 존재 여부
	 */
	public boolean exists(Integer userId, String accountNo) {
		Boolean hasKey = redisTemplate.hasKey(buildKey(userId, accountNo) + FIELD_USER_KEY);
		return Boolean.TRUE.equals(hasKey);
	}

	private String buildKey(Integer userId, String accountNo) {
		return KEY_PREFIX + userId + ":" + accountNo;
	}
}
