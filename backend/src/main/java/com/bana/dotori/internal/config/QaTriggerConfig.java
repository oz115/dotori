package com.bana.dotori.internal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QaTriggerProperties.class)
public class QaTriggerConfig {
}
