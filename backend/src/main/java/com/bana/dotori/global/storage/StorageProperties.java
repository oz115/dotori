package com.bana.dotori.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 인증 사진 저장소 설정 (CHORE-13).
 *
 * <p>버킷은 비공개다. 앱만 액세스 키로 접근하고, 아이·부모 화면은 서버가 발급한
 * presigned URL 로 본다. 앱에 액세스 키를 넣으면 그 키는 공개된 키가 되고, 퍼블릭 버킷보다
 * 나쁘다 — 읽기뿐 아니라 쓰기·삭제까지 열린다.
 *
 * @param accessKeyId     비면 AWS SDK 기본 자격증명 탐색으로 넘어간다 (EC2 IAM 역할 대비)
 * @param secretAccessKey 위와 같다
 * @param bucket          팀 공용 버킷이라 기본값을 둔다
 * @param region          서울 리전
 * @param presignedUrlTtlSeconds presigned URL 유효 시간
 */
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
	@DefaultValue("") String accessKeyId,
	@DefaultValue("") String secretAccessKey,
	@DefaultValue("s15p21a505dotori") String bucket,
	@DefaultValue("ap-northeast-2") String region,
	@DefaultValue("600") long presignedUrlTtlSeconds
) {

	/**
	 * 정적 자격증명을 쓸지 판단한다.
	 *
	 * <p>{@code .env} 는 스프링 설정으로 import 되므로 OS 환경변수가 아니다. AWS SDK 의 기본
	 * 자격증명 탐색은 환경변수·프로필·인스턴스 메타데이터만 보기 때문에 그 값을 찾지 못한다.
     * 그래서 여기서 읽어 명시적으로 넘긴다.
	 */
	public boolean hasStaticCredentials() {
		return !accessKeyId.isBlank() && !secretAccessKey.isBlank();
	}
}
