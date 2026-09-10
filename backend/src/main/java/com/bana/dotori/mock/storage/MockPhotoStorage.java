package com.bana.dotori.mock.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.bana.dotori.global.storage.PhotoStorage;

/**
 * 테스트용 {@link PhotoStorage}. 실제 S3 를 때리지 않고 메모리에 담아 둔다.
 *
 * <p>{@code MockBankTransferClient} 와 같은 자리다. 통합 테스트가 진짜 S3 를 쓰면 느려지고,
 * 액세스 키를 받지 못한 팀원은 테스트가 깨지고, 버킷에 쓰레기 파일이 쌓인다.
 *
 * <p>올린 내용을 들고 있는 이유는 테스트가 "무엇이 올라갔는지" 확인할 수 있게 하려는
 * 것이다. 스프링 컨텍스트가 테스트 클래스 사이에서 재사용되므로 필요하면 {@link #clear()}
 * 로 비운다.
 */
@Component
@Profile("test")
public class MockPhotoStorage implements PhotoStorage {

	private final Map<String, byte[]> uploaded = new ConcurrentHashMap<>();

	@Override
	public void upload(String key, byte[] content, String contentType) {
		uploaded.put(key, content);
	}

	/**
	 * 실제 presigned URL 과 같은 모양을 흉내 낸다.
	 *
	 * <p>서명 파라미터가 붙어 있다는 점이 중요하다. 호출부가 "키를 그대로 응답에 담고 있는지"
	 * 아니면 "발급을 거쳤는지" 를 이 값으로 구분할 수 있다.
	 */
	@Override
	public String presignedUrl(String key) {
		return "https://mock-storage.local/" + key + "?X-Amz-Signature=mock";
	}

	public boolean contains(String key) {
		return uploaded.containsKey(key);
	}

	public int count() {
		return uploaded.size();
	}

	public void clear() {
		uploaded.clear();
	}
}
