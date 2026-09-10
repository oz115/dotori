package com.bana.dotori.chore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bana.dotori.chore.dto.ChorePhotoUploadResponse;
import com.bana.dotori.chore.service.ChorePhotoService;

import lombok.RequiredArgsConstructor;

/**
 * 인증 사진 업로드 API (CHORE-13). 아이가 호출한다.
 *
 * <p>완료 요청과 경로가 다르므로({@code /uploads/chore-photos}) 컨트롤러를 따로 둔다.
 * 파일을 다루는 유일한 엔드포인트라 multipart 설정도 여기서만 걸린다.
 *
 * <p>갤러리 대신 카메라만 쓰게 하는 것은 클라이언트 정책이다. 서버는 올라온 파일이 촬영한
 * 것인지 고른 것인지 구분할 수 없다.
 */
@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/uploads")
@RequiredArgsConstructor
public class ChorePhotoController {

	private final ChorePhotoService chorePhotoService;

	/**
	 * 사진을 올리고 저장 키를 돌려준다.
	 *
	 * <p>응답은 키다. 이 값을 완료 요청(CHORE-11)의 {@code photoKey} 에 실어 보낸다.
	 * 미리보기는 방금 찍은 로컬 파일로 하면 되므로 URL 을 내려주지 않는다.
	 */
	@PostMapping(value = "/chore-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ChorePhotoUploadResponse> uploadChorePhoto(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId,
		@RequestPart("photo") MultipartFile photo) {
		ChorePhotoUploadResponse response = chorePhotoService.upload(userId, familyId, childId, photo);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
