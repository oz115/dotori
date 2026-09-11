package com.bana.dotori.investment.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

	@Bean
	public RestClient aiRestClient(AiProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		// 뉴스 3건을 LLM 으로 생성하고 가족 수만큼 가격 이력을 쓰는 작업이라
		// 금융망 호출(10초)보다 훨씬 오래 걸릴 수 있다
		factory.setReadTimeout(Duration.ofMinutes(3));

		return RestClient.builder()
			.baseUrl(properties.baseUrl())
			.requestFactory(factory)
			// AI 서버의 require_internal_token 이 확인하는 공유 토큰 헤더
			.defaultHeader("X-Internal-Token", properties.internalToken())
			.build();
	}
}
