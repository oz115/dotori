package com.bana.dotori.chore.service;

import org.springframework.web.multipart.MultipartFile;

import com.bana.dotori.chore.dto.ChorePhotoUploadResponse;

/**
 * 인증 사진 업로드 (CHORE-13).
 *
 * <p>완료 요청(CHORE-11)과 두 번의 호출로 나뉘어 있다. 업로드만 하고 요청을 안 보내면
 * 사진이 저장소에 고아로 남는데, 명세가 그렇게 정해져 있어 구조적으로 생기는 일이다.
 * 정리는 저장소 라이프사이클 규칙으로 다룬다 (이번 범위 밖).
 */
public interface ChorePhotoService {

	ChorePhotoUploadResponse upload(Integer userId, Integer familyId, Integer childId, MultipartFile photo);
}
