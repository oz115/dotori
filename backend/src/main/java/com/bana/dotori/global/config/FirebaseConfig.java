package com.bana.dotori.global.config;

import java.io.FileInputStream;
import java.io.IOException;

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

	@Bean
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
