package com.bana.dotori.global.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
@EnableConfigurationProperties(FcmProperties.class)
public class FirebaseConfig {

	/**
	 * FCM 자격증명 경로가 채워진 환경에서만 만든다.
	 *
	 * <p>키가 없다고 애플리케이션 전체가 뜨지 못하면 푸시 하나 때문에 로그인·집안일·상점이
	 * 다 멈춘다. 실제로 배포 환경에서 이 경로가 비어 502 가 났고, 테스트 설정에도 키가 없어
	 * 컨텍스트를 띄우는 테스트가 전부 실패했다.
	 *
	 * <p>{@code @ConditionalOnProperty} 로는 못 거른다 — {@code fcm.credentials-path} 는
	 * {@code ${FCM_CREDENTIALS_PATH:}} 라 값이 비어도 "설정됨" 으로 잡힌다. 값 자체가 비었는지를 본다.
	 *
	 * <p>빈이 없으면 발송만 건너뛴다. 알림함 기록은 그대로 남는다
	 * ({@code PushNotificationServiceImpl} 이 빈 부재를 견딘다).
	 */
	@Bean
	@ConditionalOnExpression("!'${fcm.credentials-path:}'.isBlank()")
	public FirebaseMessaging firebaseMessaging(FcmProperties properties) throws IOException {
		try (FileInputStream credentials = new FileInputStream(properties.credentialsPath())) {
			FirebaseOptions options = FirebaseOptions.builder()
				.setCredentials(GoogleCredentials.fromStream(credentials))
				.build();

			FirebaseApp app = FirebaseApp.getApps().isEmpty()
				? FirebaseApp.initializeApp(options)
				: FirebaseApp.getInstance();

			return FirebaseMessaging.getInstance(app);
		}
	}
}
