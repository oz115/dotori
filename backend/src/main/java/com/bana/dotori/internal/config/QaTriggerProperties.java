package com.bana.dotori.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 임시 QA 트리거 컨트롤러 설정.
 *
 * <p>{@code enabled} 가 false(기본값)면 {@code QaTriggerController} 빈 자체가 생기지 않는다 —
 * 운영 환경엔 {@code QA_TRIGGER_ENABLED} 를 절대 넣지 않는 방식으로 막는다.
 *
 * <p>{@code token} 은 새 시크릿을 늘리지 않고 AI 서버 호출에 이미 쓰는 {@code INTERNAL_TOKEN}
 * 값을 그대로 공유한다.
 */
@ConfigurationProperties(prefix = "qa-trigger")
public record QaTriggerProperties(
	@DefaultValue("false") boolean enabled,
	@DefaultValue("") String token
) {
}
