package com.bana.dotori.chore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.chore.dto.ChorePhotoUploadResponse;
import com.bana.dotori.chore.service.ChorePhotoService;
import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;

/**
 * 3계층 - 업로드 엔드포인트의 인증·상태 코드만 검증한다 (CHORE-13).
 *
 * <p>파일을 받는 유일한 엔드포인트라 multipart 로 들어온 요청이 실제로 컨트롤러에 닿는지,
 * 그리고 파트 이름이 틀렸을 때 500 이 아니라 400 이 나가는지를 여기서 본다.
 */
@WebMvcTest(ChorePhotoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class ChorePhotoControllerTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final String URL = "/api/v1/families/10/children/2/uploads/chore-photos";
	private static final String PHOTO_KEY = "chores/10/2/550e8400-e29b-41d4-a716-446655440000.jpg";
	private static final String TOKEN = "access-token";
	private static final String BEARER = "Bearer " + TOKEN;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChorePhotoService chorePhotoService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated() {
		given(jwtTokenProvider.getAuthentication(TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(CHILD_ID, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	private MockMultipartFile file(String partName) {
		return new MockMultipartFile(partName, "IMG_0001.jpg", "image/jpeg", "사진 내용".getBytes());
	}

	@Test
	@DisplayName("사진을 올리면 201과 저장 키를 반환한다")
	void uploadChorePhoto_created() throws Exception {
		givenAuthenticated();
		given(chorePhotoService.upload(any(), any(), any(), any()))
			.willReturn(new ChorePhotoUploadResponse(PHOTO_KEY));

		mockMvc.perform(multipart(URL)
				.file(file("photo"))
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.photoKey").value(PHOTO_KEY));

		// 경로의 아이와 토큰의 사용자가 서비스로 그대로 전달돼야 한다.
		verify(chorePhotoService).upload(eq(CHILD_ID), eq(FAMILY_ID), eq(CHILD_ID), any());
	}

	@Test
	@DisplayName("응답에 URL 은 없다")
	void uploadChorePhoto_keyOnly() throws Exception {
		givenAuthenticated();
		given(chorePhotoService.upload(any(), any(), any(), any()))
			.willReturn(new ChorePhotoUploadResponse(PHOTO_KEY));

		// 비공개 버킷이라 URL 을 줘도 볼 수 없고, presigned URL 은 몇 분 뒤 만료된다.
		// 프론트가 URL 을 기대하고 붙지 않도록 계약을 여기서 못박아 둔다.
		mockMvc.perform(multipart(URL)
				.file(file("photo"))
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.photoUrl").doesNotExist());
	}

	@Test
	@DisplayName("토큰이 없으면 401을 반환한다")
	void uploadChorePhoto_unauthorized() throws Exception {
		mockMvc.perform(multipart(URL)
				.file(file("photo")))
			.andExpect(status().isUnauthorized());

		verify(chorePhotoService, never()).upload(any(), any(), any(), any());
	}

	@Test
	@DisplayName("파트 이름이 photo 가 아니면 400을 반환한다")
	void uploadChorePhoto_wrongPartName() throws Exception {
		givenAuthenticated();

		// 핸들러가 없으면 500 이 나가고 프론트는 서버 장애로 오해한다. 파트 이름은
		// 클라이언트가 고칠 수 있는 문제라 400 이어야 한다.
		mockMvc.perform(multipart(URL)
				.file(file("file"))
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_FILE"));

		verify(chorePhotoService, never()).upload(any(), any(), any(), any());
	}

	@Test
	@DisplayName("파일이 잘못됐으면 400과 INVALID_FILE 을 반환한다")
	void uploadChorePhoto_invalidFile() throws Exception {
		givenAuthenticated();
		given(chorePhotoService.upload(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.INVALID_FILE));

		mockMvc.perform(multipart(URL)
				.file(file("photo"))
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_FILE"));
	}

	@Test
	@DisplayName("부모가 올리려 하면 403을 반환한다")
	void uploadChorePhoto_forbidden() throws Exception {
		givenAuthenticated();
		given(chorePhotoService.upload(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(multipart(URL)
				.file(file("photo"))
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("남의 가족 경로로 올리려 하면 404를 반환한다")
	void uploadChorePhoto_notFound() throws Exception {
		givenAuthenticated();
		given(chorePhotoService.upload(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		mockMvc.perform(multipart(URL)
				.file(file("photo"))
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FAMILY_NOT_FOUND"));
	}

	@Test
	@DisplayName("저장소가 실패하면 502와 FILE_UPLOAD_FAILED 를 반환한다")
	void uploadChorePhoto_storageFailure() throws Exception {
		givenAuthenticated();
		given(chorePhotoService.upload(any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.FILE_UPLOAD_FAILED));

		// 우리 잘못이 아니라 외부 저장소가 죽은 경우라 5xx 가 맞다. 다만 500 과 구분해
		// 502 로 내려 프론트가 "잠시 후 다시" 안내를 띄울 수 있게 한다.
		mockMvc.perform(multipart(URL)
				.file(file("photo"))
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.code").value("FILE_UPLOAD_FAILED"));
	}
}
