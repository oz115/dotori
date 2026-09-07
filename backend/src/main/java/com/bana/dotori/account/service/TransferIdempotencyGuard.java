package com.bana.dotori.account.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransferIdempotencyGuard {

	private static final String KEY_PREFIX = "account:transfer:idem:";
	private static final Duration TTL = Duration.ofMinutes(5);

	private final StringRedisTemplate redisTemplate;

	/**
	 * 이체 요청의 중복 제출을 방지한다.
	 * idempotencyKey가 없으면 검사를 건너뛴다.
	 * 이미 예약된 키라면 DUPLICATE_TRANSFER_REQUEST 예외를 발생시킨다.
	 *
	 * @param fromAccountId 출금 계좌 ID
	 * @param idempotencyKey 클라이언트가 전달한 idempotency 키
	 */
	public void reserve(Long fromAccountId, String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return;
		}
		String key = KEY_PREFIX + fromAccountId + ":" + idempotencyKey;
		Boolean reserved = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
		if (!Boolean.TRUE.equals(reserved)) {
			throw new BusinessException(ErrorCode.DUPLICATE_TRANSFER_REQUEST);
		}
	}
}
