package com.bana.dotori.notification.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.notification.dto.NotificationItem;
import com.bana.dotori.notification.dto.NotificationListResponse;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.NotificationService;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationService notificationService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private static final String NOTIFICATIONS_URL = "/api/v1/notifications";
	private static final String ACCESS_TOKEN = "user-access-token";
	private static final String BEARER = "Bearer " + ACCESS_TOKEN;

	private void givenAuthenticated(Integer userId) {
		given(jwtTokenProvider.getAuthentication(ACCESS_TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	@Test
	@DisplayName("조회 성공 시 200과 알림 목록/페이지 정보를 반환한다")
	void getNotifications_success() throws Exception {
		givenAuthenticated(1);
		NotificationListResponse response = new NotificationListResponse(
			List.of(new NotificationItem(1, NotificationType.CASHOUT, "환전 요청", "김아이님이 10,000P 환전을 요청했어요", 1, false,
				OffsetDateTime.of(2026, 8, 28, 12, 0, 0, 0, ZoneOffset.ofHours(9)))),
			0, 1, 3);
		given(notificationService.getNotifications(1, null, null, 0, 20)).willReturn(response);

		mockMvc.perform(get(NOTIFICATIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.notifications[0].notificationId").value(1))
			.andExpect(jsonPath("$.notifications[0].type").value("CASHOUT"))
			.andExpect(jsonPath("$.notifications[0].isRead").value(false))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.unreadCount").value(3));
	}

	@Test
	@DisplayName("쿼리 파라미터가 없으면 기본값(page=0, size=20)으로 서비스를 호출한다")
	void getNotifications_defaultsPageAndSize() throws Exception {
		givenAuthenticated(1);
		given(notificationService.getNotifications(1, null, null, 0, 20))
			.willReturn(new NotificationListResponse(List.of(), 0, 0, 0));

		mockMvc.perform(get(NOTIFICATIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk());

		then(notificationService).should().getNotifications(1, null, null, 0, 20);
	}

	@Test
	@DisplayName("type/isRead/page/size 파라미터를 그대로 서비스에 전달한다")
	void getNotifications_passesFilters() throws Exception {
		givenAuthenticated(1);
		given(notificationService.getNotifications(eq(1), eq(NotificationType.PURCHASE), eq(false), eq(1), eq(10)))
			.willReturn(new NotificationListResponse(List.of(), 1, 2, 0));

		mockMvc.perform(get(NOTIFICATIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("type", "PURCHASE")
				.param("isRead", "false")
				.param("page", "1")
				.param("size", "10"))
			.andExpect(status().isOk());

		then(notificationService).should().getNotifications(1, NotificationType.PURCHASE, false, 1, 10);
	}

	@Test
	@DisplayName("type이 정의되지 않은 값이면 400과 INVALID_REQUEST 에러를 반환한다")
	void getNotifications_invalidType() throws Exception {
		givenAuthenticated(1);

		mockMvc.perform(get(NOTIFICATIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("type", "NOT_A_TYPE"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(notificationService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("인증 토큰이 없으면 401을 반환한다")
	void getNotifications_unauthenticated() throws Exception {
		mockMvc.perform(get(NOTIFICATIONS_URL))
			.andExpect(status().isUnauthorized());

		then(notificationService).shouldHaveNoInteractions();
	}
}
