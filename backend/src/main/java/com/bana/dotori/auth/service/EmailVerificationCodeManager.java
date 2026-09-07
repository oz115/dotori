package com.bana.dotori.auth.service;

import java.time.Duration;
import java.util.UUID;

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
	private static final String FIELD_TOKEN = ":token";
	private static final long CODE_TTL_SECONDS = 600L;
	private static final long COOLDOWN_SECONDS = 60L;
	private static final long TOKEN_TTL_SECONDS = 600L;
	private static final int MAX_ATTEMPT = 5;

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

	/**
	 * 저장된 코드와 대조한다. 성공 시 코드를 지우고 후속 요청용 verificationToken 을 발급한다.
	 *
	 * <p>실패할 때마다 시도 횟수를 올리고, {@value MAX_ATTEMPT}회 이상이면 코드를 지워
	 * 재발송을 강제한다 (TOO_MANY_ATTEMPTS).
	 *
	 * @param email 코드를 받은 이메일 (소문자 정규화된 값)
	 * @param code  사용자가 입력한 6자리 코드
	 * @return 후속 API(회원가입/아이디 찾기/비밀번호 재설정)에 전달할 verificationToken
	 */
	public String verifyCode(String email, String code) {
		String stored = redisTemplate.opsForValue().get(buildKey(email) + FIELD_CODE);
		if (stored == null) {
			throw new BusinessException(ErrorCode.INVALID_CODE);
		}
		if (!stored.equals(code)) {
			incrementAttempt(email);
			throw new BusinessException(ErrorCode.INVALID_CODE);
		}

		redisTemplate.delete(buildKey(email) + FIELD_CODE);
		redisTemplate.delete(buildKey(email) + FIELD_ATTEMPT);

		String token = "vt_" + UUID.randomUUID().toString().replace("-", "");
		redisTemplate.opsForValue()
			.set(buildKey(email) + FIELD_TOKEN, token, Duration.ofSeconds(TOKEN_TTL_SECONDS));
		return token;
	}

	/**
	 * verificationToken 이 유효한지 확인한다. 만료되었거나 값이 다르면
	 * INVALID_VERIFICATION_TOKEN 예외를 발생시킨다.
	 *
	 * <p>여기서 지우지 않는 이유: 후속 검증(회원 조회 등)이 실패해도 토큰은 살아 있어야
	 * 사용자가 인증을 처음부터 다시 하지 않는다. 1회용 처리는 성공 시점에
	 * {@link #deleteToken}으로 한다.
	 *
	 * @param email 인증을 완료한 이메일 (소문자 정규화된 값)
	 * @param token 클라이언트가 제출한 verificationToken
	 */
	public void requireValidToken(String email, String token) {
		String stored = redisTemplate.opsForValue().get(buildKey(email) + FIELD_TOKEN);
		if (stored == null || !stored.equals(token)) {
			throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
		}
	}

	/**
	 * verificationToken 을 삭제한다 (1회용 소모, 후속 API 성공 시 호출).
	 *
	 * @param email 인증을 완료한 이메일 (소문자 정규화된 값)
	 */
	public void deleteToken(String email) {
		redisTemplate.delete(buildKey(email) + FIELD_TOKEN);
	}

	private void incrementAttempt(String email) {
		Long current = redisTemplate.opsForValue().increment(buildKey(email) + FIELD_ATTEMPT);
		if (current == null || current >= MAX_ATTEMPT) {
			log.warn("이메일 인증 시도 횟수 초과 - email: {}", email);
			redisTemplate.delete(buildKey(email) + FIELD_CODE);
			redisTemplate.delete(buildKey(email) + FIELD_ATTEMPT);
			throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS);
		}
	}

	private String buildKey(String email) {
		return KEY_PREFIX + email;
	}
}
