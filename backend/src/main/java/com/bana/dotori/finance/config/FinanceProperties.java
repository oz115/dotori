package com.bana.dotori.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "finance")
public record FinanceProperties(
	String baseUrl,
	String apiKey,
	@DefaultValue("00100") String institutionCode,
	@DefaultValue("001") String fintechAppNo,
	@DefaultValue("도토리") String authText
) {
}
