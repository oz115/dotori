package com.bana.dotori.chore.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bana.dotori.chore.dto.ChorePhotoUploadResponse;
import com.bana.dotori.chore.entity.ChorePhotoKey;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.storage.PhotoStorage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChorePhotoServiceImpl implements ChorePhotoService {

	/**
	 * 받아 줄 최대 크기.
	 *
	 * <p>{@code spring.servlet.multipart.max-file-size} 와 같은 값이어야 한다. 스프링 쪽이 더
	 * 작으면 이 검사에 닿기 전에 요청이 끊기고(그건 핸들러가 400 으로 바꿔 준다), 더 크면
	 * 여기서 걸러 낸다. 두 곳에 있는 이유는 스프링이 컨트롤러 진입 전에 끊어 주는 값이고
	 * 이것은 서비스 규칙이라, 한쪽만 두면 다른 경로로 들어올 때 검사가 빠지기 때문이다.
	 */
	private static final long MAX_PHOTO_SIZE_BYTES = 10L * 1024 * 1024;

	private final ChoreAccessValidator accessValidator;
	private final PhotoStorage photoStorage;

	/**
	 * 아이가 인증 사진을 올린다 (CHORE-13).
	 *
	 * <p>키는 서버가 만든다. 원본 파일명은 쓰지 않는다 — 클라이언트가 정하는 값이라 경로
	 * 조작이 섞일 수 있고, 파일명 자체가 쓸모 있는 정보도 아니다.
	 *
	 * <p>업로드 자체는 할 일(chore)과 무관하다. 어느 집안일에 붙일지는 완료 요청(CHORE-11)이
	 * 정하므로 여기서 choreId 를 받지 않는다. 덕분에 아이가 사진을 먼저 찍고 어느 할 일에
	 * 붙일지 나중에 고르는 화면도 가능하다.
	 */
	@Override
	public ChorePhotoUploadResponse upload(Integer userId, Integer familyId, Integer childId, MultipartFile photo) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireChild(userId);

		validate(photo);

		String key = ChorePhotoKey.issue(familyId, childId, photo.getContentType());
		photoStorage.upload(key, readBytes(photo), photo.getContentType());

		return new ChorePhotoUploadResponse(key);
	}

	private void validate(MultipartFile photo) {
		if (photo == null || photo.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_FILE, "사진 파일이 비어 있습니다");
		}
		if (!ChorePhotoKey.isSupported(photo.getContentType())) {
			throw new BusinessException(ErrorCode.INVALID_FILE,
					"jpg 또는 png 만 올릴 수 있습니다. contentType=" + photo.getContentType());
		}
		if (photo.getSize() > MAX_PHOTO_SIZE_BYTES) {
			throw new BusinessException(ErrorCode.INVALID_FILE,
					"사진은 10MB 이하여야 합니다. size=" + photo.getSize());
		}
	}

	/**
	 * 파일을 바이트로 읽는다.
	 *
	 * <p>10MB 를 메모리에 한 번 올린다. 스트림으로 그대로 흘려보내면 메모리를 덜 쓰지만,
	 * S3 SDK 가 길이를 미리 알아야 해서 어차피 크기를 함께 넘겨야 하고, 동시 업로드가
	 * 수십 건 수준인 서비스라 이 단순함을 택했다. 트래픽이 늘면 스트리밍으로 바꾼다.
	 */
	private byte[] readBytes(MultipartFile photo) {
		try {
			return photo.getBytes();
		} catch (IOException e) {
			// 전송이 중간에 끊긴 경우다. 클라이언트가 다시 올리면 되는 상황이라 400 이다.
			throw new BusinessException(ErrorCode.INVALID_FILE, "사진을 읽지 못했습니다", e);
		}
	}
}
