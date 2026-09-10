package com.bana.dotori.global.storage;

/**
 * 사진 파일 저장소 경계 (CHORE-13).
 *
 * <p>키를 만드는 일은 여기 없다. 어떤 경로에 무슨 이름으로 넣을지는 도메인 규칙이라
 * {@code ChorePhotoKey} 가 정하고, 이 인터페이스는 "이 키에 이 바이트를 넣어라",
 * "이 키를 볼 수 있는 링크를 만들어라" 만 안다.
 *
 * <p>인터페이스를 둔 이유는 테스트다. 실제 S3 를 때리면 느리고, 액세스 키가 없는 팀원은
 * 테스트가 깨지고, 버킷에 쓰레기 파일이 쌓인다. 금융망을 {@code BankTransferClient} 로
 * 감싸 둔 것과 같은 방식으로 프로파일에 따라 구현체를 갈아끼운다
 * ({@code S3PhotoStorage} / {@code MockPhotoStorage}).
 */
public interface PhotoStorage {

	/**
	 * 주어진 키에 파일을 올린다. 같은 키가 이미 있으면 덮어쓴다.
	 *
	 * <p>키에 UUID 가 들어가므로 실제로 덮어쓰는 일은 없다. 덮어쓰기를 막지 않는 이유는
	 * 막아도 얻는 게 없고, S3 에서 그것을 막으려면 조회를 한 번 더 해야 하기 때문이다.
	 *
	 * @param key         저장 키. {@code chores/{familyId}/{childId}/{uuid}.jpg} 형태
	 * @param content     파일 내용
	 * @param contentType MIME 타입. 나중에 presigned URL 로 받을 때 브라우저가 이걸 보고 렌더한다
	 * @throws com.bana.dotori.global.error.BusinessException FILE_UPLOAD_FAILED 저장에 실패한 경우
	 */
	void upload(String key, byte[] content, String contentType);

	/**
	 * 그 키를 볼 수 있는 임시 링크를 만든다.
	 *
	 * <p>버킷이 비공개라 이 링크 없이는 아무도 사진을 볼 수 없다. 링크에는 서명과 만료시각이
	 * 붙어 있어 500자를 넘고 몇 분 뒤 죽는다 — <b>저장하지 말고 응답에 담을 때마다 새로 만든다.</b>
	 *
	 * @param key 저장 키
	 * @return 유효시간이 붙은 GET URL
	 */
	String presignedUrl(String key);
}
