package com.bana.dotori.global.storage;

import java.time.Duration;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * 실제 S3 에 사진을 올리고 presigned URL 을 발급하는 {@link PhotoStorage} 구현체.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class S3PhotoStorage implements PhotoStorage {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final StorageProperties properties;

	@Override
	public void upload(String key, byte[] content, String contentType) {
		try {
			s3Client.putObject(PutObjectRequest.builder()
					.bucket(properties.bucket())
					.key(key)
					.contentType(contentType)
					.build(),
				RequestBody.fromBytes(content));
		} catch (SdkException e) {
			// 자격증명 누락·권한 부족·네트워크 오류가 모두 여기로 온다. 원인은 로그로 남기고
			// 사용자에게는 502 를 준다 — 아이가 고칠 수 있는 문제가 아니라 400 은 맞지 않는다.
			log.error("인증 사진 업로드 실패. bucket={}, key={}", properties.bucket(), key, e);
			throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "사진 저장에 실패했습니다. key=" + key, e);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>발급은 서명 계산뿐이라 네트워크를 타지 않는다. 그래서 목록 응답에서 건마다 불러도
	 * 왕복 비용이 없다. 다만 키가 실재하는지도 확인하지 않으므로, 없는 키로 만든 링크는
	 * 열었을 때 404 가 난다.
	 */
	@Override
	public String presignedUrl(String key) {
		return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofSeconds(properties.presignedUrlTtlSeconds()))
				.getObjectRequest(GetObjectRequest.builder()
					.bucket(properties.bucket())
					.key(key)
					.build())
				.build())
			.url()
			.toString();
	}
}
