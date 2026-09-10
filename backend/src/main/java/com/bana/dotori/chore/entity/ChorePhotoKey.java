package com.bana.dotori.chore.entity;

import java.util.UUID;

/**
 * 인증 사진의 저장 키 형식 (CHORE-13).
 *
 * <pre>
 * chores/{familyId}/{childId}/{uuid}.{jpg|png}
 * </pre>
 *
 * <p>DB 에는 이 키를 저장한다. URL 을 저장하지 않는 이유는 버킷이 비공개라 URL 로는 아무도
 * 볼 수 없고, 볼 수 있는 presigned URL 은 500자가 넘고 몇 분 뒤 만료돼 저장할 값이 아니기
 * 때문이다. 볼 때마다 이 키로 새 링크를 만든다.
 *
 * <p>만드는 곳과 검사하는 곳을 한 파일에 둔 이유는 {@code Chore#rewardIdempotencyKey()} 와
 * 같다 — 형식이 한 곳에만 있어야 한쪽만 고치는 일이 없다.
 *
 * <p>경로에 familyId·childId 를 넣은 것은 IAM 정책과 검증 때문이다. IAM 은
 * {@code chores/*} 만 허용해 키가 새더라도 다른 폴더는 닫혀 있고, 완료 요청(CHORE-11)은
 * 클라이언트가 보낸 키가 <b>자기 경로</b>인지 확인할 수 있다.
 */
public final class ChorePhotoKey {

	/** IAM 정책의 {@code arn:aws:s3:::{bucket}/chores/*} 와 같은 값이어야 한다. */
	private static final String PREFIX = "chores";

	private static final String JPEG_MIME = "image/jpeg";
	private static final String PNG_MIME = "image/png";

	private ChorePhotoKey() {
	}

	/**
	 * 새 사진의 키를 만든다. 파일명은 UUID 라 같은 키가 두 번 나오지 않는다.
	 *
	 * <p>아이가 올린 원본 파일명은 쓰지 않는다. 클라이언트가 정하는 값이라 경로 조작
	 * ({@code ../}) 이나 이상한 문자가 섞일 수 있고, 파일명 자체가 쓸모 있는 정보도 아니다.
	 *
	 * @param contentType {@code image/jpeg} 또는 {@code image/png}
	 * @throws IllegalArgumentException 지원하지 않는 타입인 경우
	 */
	public static String issue(Integer familyId, Integer childId, String contentType) {
		return PREFIX + "/" + familyId + "/" + childId + "/" + UUID.randomUUID() + "." + extensionOf(contentType);
	}

	/**
	 * 그 키가 이 가족·아이의 것인지 본다 (CHORE-11 검증).
	 *
	 * <p>완료 요청의 {@code photoKey} 는 클라이언트가 보내는 문자열이라 그대로 믿으면 안 된다.
	 * 남의 사진 키를 넣어 자기 인증으로 쓰거나, 우리 버킷 밖의 값을 넣을 수 있다.
	 * 업로드할 때 서버가 만든 키만 통과하도록 형식과 소유를 함께 확인한다.
	 */
	public static boolean belongsTo(String key, Integer familyId, Integer childId) {
		if (key == null) {
			return false;
		}
		String[] parts = key.split("/");
		if (parts.length != 4 || !PREFIX.equals(parts[0])) {
			return false;
		}
		if (!String.valueOf(familyId).equals(parts[1]) || !String.valueOf(childId).equals(parts[2])) {
			return false;
		}
		return parts[3].endsWith(".jpg") || parts[3].endsWith(".png");
	}

	/**
	 * 허용하는 MIME 타입인지 본다.
	 *
	 * <p>클라이언트가 보낸 Content-Type 을 믿는 검사다. 파일 내용을 뜯어보지는 않으므로
	 * 확장자만 바꾼 파일은 통과한다. 카메라로 찍은 사진만 올라오는 화면이고, 버킷이
	 * 비공개라 올라간 파일을 임의로 실행·배포할 경로도 없어 이 수준에서 멈춘다.
	 */
	public static boolean isSupported(String contentType) {
		return JPEG_MIME.equals(contentType) || PNG_MIME.equals(contentType);
	}

	private static String extensionOf(String contentType) {
		if (JPEG_MIME.equals(contentType)) {
			return "jpg";
		}
		if (PNG_MIME.equals(contentType)) {
			return "png";
		}
		throw new IllegalArgumentException("지원하지 않는 이미지 타입입니다: " + contentType);
	}
}
