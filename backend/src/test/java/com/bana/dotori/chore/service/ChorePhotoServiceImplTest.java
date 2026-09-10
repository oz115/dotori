package com.bana.dotori.chore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.bana.dotori.chore.dto.ChorePhotoUploadResponse;
import com.bana.dotori.chore.entity.ChorePhotoKey;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.storage.PhotoStorage;

/**
 * 2계층 - 인증 사진 업로드 규칙만 검증한다 (CHORE-13).
 *
 * <p>S3 는 {@link PhotoStorage} 인터페이스로 감싸 두었으므로 여기서는 모킹한다. 실제 버킷에
 * 붙지 않으니 자격증명도 네트워크도 필요 없다. 이 테스트가 실패하면 원인은 업로드 규칙 쪽이다.
 *
 * <p>키 형식 자체는 {@code ChorePhotoKeyTest} 가 덮는다. 여기서는 "서버가 만든 키를 저장소에
 * 넘기고 그 값을 그대로 응답한다"만 본다.
 */
@DisplayName("ChorePhotoServiceImpl 인증 사진 업로드")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChorePhotoServiceImplTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final long MAX_SIZE = 10L * 1024 * 1024;

	@Mock
	private ChoreAccessValidator accessValidator;

	@Mock
	private PhotoStorage photoStorage;

	@InjectMocks
	private ChorePhotoServiceImpl chorePhotoService;

	private MockMultipartFile photo(String contentType) {
		return new MockMultipartFile("photo", "IMG_0001.jpg", contentType, "사진 내용".getBytes());
	}

	private ChorePhotoUploadResponse upload(MultipartFile photo) {
		return chorePhotoService.upload(CHILD_ID, FAMILY_ID, CHILD_ID, photo);
	}

	@Nested
	@DisplayName("아이가 사진을 올리면")
	class Upload {

		@Test
		@DisplayName("저장소에 올리고 그 키를 돌려준다")
		void 키를_돌려준다() {
			MockMultipartFile photo = photo("image/jpeg");

			ChorePhotoUploadResponse response = upload(photo);

			// 응답의 키와 저장소에 올린 키가 같아야 한다. 다르면 아이가 받은 키로
			// 완료 요청을 올려도 사진이 없는 곳을 가리킨다.
			verify(photoStorage).upload(eq(response.photoKey()), any(), eq("image/jpeg"));
		}

		@Test
		@DisplayName("서버가 만든 키를 쓴다 - 원본 파일명은 쓰지 않는다")
		void 파일명을_쓰지_않는다() {
			MockMultipartFile photo = new MockMultipartFile("photo", "../../etc/passwd", "image/jpeg",
				"사진 내용".getBytes());

			ChorePhotoUploadResponse response = upload(photo);

			// 클라이언트가 정한 파일명이 키에 섞이면 경로 조작이 가능해진다.
			assertThat(response.photoKey()).doesNotContain("passwd").doesNotContain("..");
			assertThat(ChorePhotoKey.belongsTo(response.photoKey(), FAMILY_ID, CHILD_ID)).isTrue();
		}

		@Test
		@DisplayName("파일 내용을 그대로 넘긴다")
		void 내용을_그대로_넘긴다() {
			byte[] content = "사진 내용".getBytes();
			MockMultipartFile photo = new MockMultipartFile("photo", "IMG_0001.jpg", "image/jpeg", content);

			upload(photo);

			ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
			verify(photoStorage).upload(any(), captor.capture(), any());
			assertThat(captor.getValue()).isEqualTo(content);
		}

		@Test
		@DisplayName("png 도 올릴 수 있다")
		void png도_된다() {
			ChorePhotoUploadResponse response = upload(photo("image/png"));

			assertThat(response.photoKey()).endsWith(".png");
		}
	}

	@Nested
	@DisplayName("권한을 볼 때")
	class Access {

		@Test
		@DisplayName("가족 경계를 먼저 확인한다")
		void 경계를_먼저_본다() {
			willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND))
				.given(accessValidator).validate(CHILD_ID, FAMILY_ID, CHILD_ID);

			assertThatThrownBy(() -> upload(photo("image/jpeg")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.FAMILY_NOT_FOUND);

			// 남의 가족 경로로 들어온 요청이 우리 버킷에 파일을 남기면 안 된다.
			verify(photoStorage, never()).upload(any(), any(), any());
		}

		@Test
		@DisplayName("부모가 올리려 하면 403 이고 저장소를 건드리지 않는다")
		void 부모는_못_올린다() {
			willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(accessValidator).requireChild(CHILD_ID);

			assertThatThrownBy(() -> upload(photo("image/jpeg")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);

			verify(photoStorage, never()).upload(any(), any(), any());
		}
	}

	@Nested
	@DisplayName("파일을 검사할 때")
	class Validation {

		@Test
		@DisplayName("빈 파일이면 INVALID_FILE 이다")
		void 빈_파일() {
			MockMultipartFile empty = new MockMultipartFile("photo", "IMG_0001.jpg", "image/jpeg", new byte[0]);

			assertThatThrownBy(() -> upload(empty))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);

			verify(photoStorage, never()).upload(any(), any(), any());
		}

		@Test
		@DisplayName("jpg·png 가 아니면 INVALID_FILE 이다")
		void 지원하지_않는_형식() {
			assertThatThrownBy(() -> upload(photo("image/gif")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);

			assertThatThrownBy(() -> upload(photo("application/pdf")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);

			// 이 검사가 없으면 ChorePhotoKey.issue 가 IllegalArgumentException 을 던져 500 이 나간다.
			verify(photoStorage, never()).upload(any(), any(), any());
		}

		@Test
		@DisplayName("Content-Type 이 없으면 INVALID_FILE 이다")
		void 타입이_없으면() {
			MockMultipartFile noType = new MockMultipartFile("photo", "IMG_0001.jpg", null, "사진".getBytes());

			assertThatThrownBy(() -> upload(noType))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
		}

		@Test
		@DisplayName("10MB 를 넘으면 INVALID_FILE 이다")
		void 용량_초과(@Mock MultipartFile big) {
			// 실제로 10MB 를 만들지 않는다. 크기만 보는 검사다.
			given(big.isEmpty()).willReturn(false);
			given(big.getContentType()).willReturn("image/jpeg");
			given(big.getSize()).willReturn(MAX_SIZE + 1);

			assertThatThrownBy(() -> upload(big))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);

			verify(photoStorage, never()).upload(any(), any(), any());
		}

		@Test
		@DisplayName("10MB 정확히는 통과한다")
		void 경계값은_통과한다(@Mock MultipartFile exact) throws Exception {
			given(exact.isEmpty()).willReturn(false);
			given(exact.getContentType()).willReturn("image/jpeg");
			given(exact.getSize()).willReturn(MAX_SIZE);
			given(exact.getBytes()).willReturn("사진".getBytes());

			assertThat(upload(exact).photoKey()).isNotBlank();
		}
	}

	@Nested
	@DisplayName("저장소가 실패하면")
	class StorageFailure {

		@Test
		@DisplayName("그 예외를 그대로 올려보낸다")
		void 전파한다() {
			willThrow(new BusinessException(ErrorCode.FILE_UPLOAD_FAILED))
				.given(photoStorage).upload(any(), any(), any());

			// 저장소가 실패했는데 키를 돌려주면 아이가 그 키로 완료 요청을 올리고
			// 부모의 대기함에 깨진 이미지가 뜬다.
			assertThatThrownBy(() -> upload(photo("image/jpeg")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
		}
	}
}
