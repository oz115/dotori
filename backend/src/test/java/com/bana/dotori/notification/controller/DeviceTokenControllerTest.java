package com.bana.dotori.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.notification.dto.DeviceTokenRegisterRequest;
import com.bana.dotori.notification.dto.DeviceTokenRegisterResponse;
import com.bana.dotori.notification.entity.DeviceTokenPlatform;
import com.bana.dotori.notification.service.DeviceTokenService;

@WebMvcTest(DeviceTokenController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class DeviceTokenControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DeviceTokenService deviceTokenService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private static final String DEVICE_TOKENS_URL = "/api/v1/device-tokens";
	private static final String ACCESS_TOKEN = "user-access-token";
	private static final String BEARER = "Bearer " + ACCESS_TOKEN;

	private void givenAuthenticated(Integer userId) {
		given(jwtTokenProvider.getAuthentication(ACCESS_TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));
	}

	@Test
	@DisplayName("디바이스 토큰 등록 성공 시 200과 등록 정보를 반환한다")
	void register_success() throws Exception {
		givenAuthenticated(1);
		DeviceTokenRegisterResponse response = new DeviceTokenRegisterResponse(1, DeviceTokenPlatform.ANDROID,
			OffsetDateTime.of(2026, 9, 9, 12, 0, 0, 0, ZoneOffset.ofHours(9)));
		given(deviceTokenService.register(eq(1), any(DeviceTokenRegisterRequest.class))).willReturn(response);

		mockMvc.perform(post(DEVICE_TOKENS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"fcmToken": "fcm-token-abc",
						"platform": "ANDROID"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.deviceTokenId").value(1))
			.andExpect(jsonPath("$.platform").value("ANDROID"))
			.andExpect(jsonPath("$.registeredAt").exists());
	}

	@Test
	@DisplayName("fcmToken이 비어 있으면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void register_blankFcmToken() throws Exception {
		givenAuthenticated(1);

		mockMvc.perform(post(DEVICE_TOKENS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"fcmToken": "",
						"platform": "ANDROID"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(deviceTokenService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("platform이 정의되지 않은 값이면 400과 INVALID_REQUEST 에러를 반환한다")
	void register_invalidPlatform() throws Exception {
		givenAuthenticated(1);

		mockMvc.perform(post(DEVICE_TOKENS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"fcmToken": "fcm-token-abc",
						"platform": "WINDOWS"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(deviceTokenService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("인증 토큰이 없으면 401을 반환한다")
	void register_unauthenticated() throws Exception {
		mockMvc.perform(post(DEVICE_TOKENS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"fcmToken": "fcm-token-abc",
						"platform": "ANDROID"
					}
					"""))
			.andExpect(status().isUnauthorized());

		then(deviceTokenService).shouldHaveNoInteractions();
	}
}
