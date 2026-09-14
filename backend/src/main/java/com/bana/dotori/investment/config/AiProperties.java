package com.bana.dotori.investment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * AI 서버(FastAPI) 내부 호출 설정. internalToken 은 AI 쪽 INTERNAL_TOKEN 과 같은 값을
 * 넣는 공유 토큰이다 — 비어 있으면 AI 서버도 검증을 건너뛴다(로컬 개발 편의).
 */
@ConfigurationProperties(prefix = "ai")
public record AiProperties(
	@DefaultValue("http://localhost:8000") String baseUrl,
	@DefaultValue("") String internalToken
) {
}
