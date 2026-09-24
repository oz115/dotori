package com.bana.dotori.global.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 클라이언트 구성 (CHORE-13).
 *
 * <p>{@code test} 프로파일에서는 빈을 만들지 않는다. 테스트에서는
 * {@code MockPhotoStorage} 가 대신 등록되므로 S3 클라이언트가 필요 없고, 만들려 하면
 * 자격증명이 없는 환경에서 컨텍스트 로딩부터 실패한다.
 */
@Configuration
@Profile("!test")
@EnableConfigurationProperties(StorageProperties.class)
public class S3Config {

	@Bean
	public S3Client s3Client(StorageProperties properties) {
		return S3Client.builder()
			.region(Region.of(properties.region()))
			.credentialsProvider(credentialsProvider(properties))
			.build();
	}

	/**
	 * presigned URL 발급 전용 클라이언트.
	 *
	 * <p>{@link S3Client} 와 따로 있어야 한다. 서명만 만들고 네트워크 호출은 하지 않는 별개
	 * 컴포넌트라 SDK 가 타입을 나눠 두었다.
	 */
	@Bean
	public S3Presigner s3Presigner(StorageProperties properties) {
		return S3Presigner.builder()
			.region(Region.of(properties.region()))
			.credentialsProvider(credentialsProvider(properties))
			.build();
	}

	/**
	 * 설정에 키가 있으면 그것을, 없으면 SDK 기본 탐색을 쓴다.
	 *
	 * <p>기본 탐색으로 넘기는 쪽은 지금 쓰지 않는다. 나중에 EC2 IAM 역할로 옮기면 키를
	 * 비우기만 하면 되도록 길을 열어 둔 것이다.
	 *
	 * <p>키가 없어도 빈 생성 자체는 성공한다. 자격증명이 없다는 사실은 실제로 S3 를 호출할 때
	 * 드러난다 — 키를 받지 못한 팀원도 앱을 띄워 다른 기능을 개발할 수 있어야 한다.
	 */
	private AwsCredentialsProvider credentialsProvider(StorageProperties properties) {
		if (properties.hasStaticCredentials()) {
			return StaticCredentialsProvider.create(
				AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey()));
		}
		return DefaultCredentialsProvider.create();
	}
}
