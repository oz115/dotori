package com.bana.dotori.notification.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.notification.dto.NotificationItem;
import com.bana.dotori.notification.dto.NotificationListResponse;
import com.bana.dotori.notification.dto.NotificationReadAllResponse;
import com.bana.dotori.notification.dto.NotificationReadResponse;
import com.bana.dotori.notification.dto.NotificationUnreadCountResponse;
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

	@Test
	@DisplayName("상세 조회 성공 시 200과 알림 정보를 반환한다")
	void getNotification_success() throws Exception {
		givenAuthenticated(1);
		NotificationItem item = new NotificationItem(1, NotificationType.CASHOUT, "환전 요청",
			"김아이님이 10,000P 환전을 요청했어요", 1, false,
			OffsetDateTime.of(2026, 8, 28, 12, 0, 0, 0, ZoneOffset.ofHours(9)));
		given(notificationService.getNotification(1, 1)).willReturn(item);

		mockMvc.perform(get(NOTIFICATIONS_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.notificationId").value(1))
			.andExpect(jsonPath("$.type").value("CASHOUT"))
			.andExpect(jsonPath("$.isRead").value(false));
	}

	@Test
	@DisplayName("본인 알림이 아니면 403과 FORBIDDEN 에러를 반환한다")
	void getNotification_forbidden() throws Exception {
		givenAuthenticated(1);
		willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(notificationService).getNotification(1, 1);

		mockMvc.perform(get(NOTIFICATIONS_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("알림이 없으면 404와 NOTIFICATION_NOT_FOUND 에러를 반환한다")
	void getNotification_notFound() throws Exception {
		givenAuthenticated(1);
		willThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND)).given(notificationService)
			.getNotification(1, 999);

		mockMvc.perform(get(NOTIFICATIONS_URL + "/999")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
	}

	@Test
	@DisplayName("인증 토큰 없이 상세 조회를 호출하면 401을 반환한다")
	void getNotification_unauthenticated() throws Exception {
		mockMvc.perform(get(NOTIFICATIONS_URL + "/1"))
			.andExpect(status().isUnauthorized());

		then(notificationService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("읽음 처리 성공 시 200과 isRead=true를 반환한다")
	void markAsRead_success() throws Exception {
		givenAuthenticated(1);
		given(notificationService.markAsRead(1, 1)).willReturn(new NotificationReadResponse(1, true));

		mockMvc.perform(patch(NOTIFICATIONS_URL + "/1/read")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.notificationId").value(1))
			.andExpect(jsonPath("$.isRead").value(true));
	}

	@Test
	@DisplayName("본인 알림이 아니면 403과 FORBIDDEN 에러를 반환한다")
	void markAsRead_forbidden() throws Exception {
		givenAuthenticated(1);
		willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(notificationService).markAsRead(1, 1);

		mockMvc.perform(patch(NOTIFICATIONS_URL + "/1/read")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("알림이 없으면 404와 NOTIFICATION_NOT_FOUND 에러를 반환한다")
	void markAsRead_notFound() throws Exception {
		givenAuthenticated(1);
		willThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND)).given(notificationService)
			.markAsRead(1, 999);

		mockMvc.perform(patch(NOTIFICATIONS_URL + "/999/read")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
	}

	@Test
	@DisplayName("인증 토큰 없이 읽음 처리를 호출하면 401을 반환한다")
	void markAsRead_unauthenticated() throws Exception {
		mockMvc.perform(patch(NOTIFICATIONS_URL + "/1/read"))
			.andExpect(status().isUnauthorized());

		then(notificationService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("전체 읽음 처리 성공 시 200과 updatedCount를 반환한다")
	void markAllAsRead_success() throws Exception {
		givenAuthenticated(1);
		given(notificationService.markAllAsRead(1)).willReturn(new NotificationReadAllResponse(3));

		mockMvc.perform(patch(NOTIFICATIONS_URL + "/read-all")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.updatedCount").value(3));
	}

	@Test
	@DisplayName("인증 토큰 없이 전체 읽음 처리를 호출하면 401을 반환한다")
	void markAllAsRead_unauthenticated() throws Exception {
		mockMvc.perform(patch(NOTIFICATIONS_URL + "/read-all"))
			.andExpect(status().isUnauthorized());

		then(notificationService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("삭제 성공 시 204를 반환한다")
	void deleteNotification_success() throws Exception {
		givenAuthenticated(1);

		mockMvc.perform(delete(NOTIFICATIONS_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNoContent());

		then(notificationService).should().deleteNotification(1, 1);
	}

	@Test
	@DisplayName("본인 알림이 아니면 403과 FORBIDDEN 에러를 반환한다")
	void deleteNotification_forbidden() throws Exception {
		givenAuthenticated(1);
		willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(notificationService).deleteNotification(1, 1);

		mockMvc.perform(delete(NOTIFICATIONS_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("알림이 없으면 404와 NOTIFICATION_NOT_FOUND 에러를 반환한다")
	void deleteNotification_notFound() throws Exception {
		givenAuthenticated(1);
		willThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND)).given(notificationService)
			.deleteNotification(1, 999);

		mockMvc.perform(delete(NOTIFICATIONS_URL + "/999")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
	}

	@Test
	@DisplayName("인증 토큰 없이 삭제를 호출하면 401을 반환한다")
	void deleteNotification_unauthenticated() throws Exception {
		mockMvc.perform(delete(NOTIFICATIONS_URL + "/1"))
			.andExpect(status().isUnauthorized());

		then(notificationService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("안 읽은 알림 개수 조회 성공 시 200과 unreadCount를 반환한다")
	void getUnreadCount_success() throws Exception {
		givenAuthenticated(1);
		given(notificationService.getUnreadCount(1)).willReturn(new NotificationUnreadCountResponse(3));

		mockMvc.perform(get(NOTIFICATIONS_URL + "/unread-count")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.unreadCount").value(3));
	}

	@Test
	@DisplayName("인증 토큰 없이 안 읽은 알림 개수 조회를 호출하면 401을 반환한다")
	void getUnreadCount_unauthenticated() throws Exception {
		mockMvc.perform(get(NOTIFICATIONS_URL + "/unread-count"))
			.andExpect(status().isUnauthorized());

		then(notificationService).shouldHaveNoInteractions();
	}
}
