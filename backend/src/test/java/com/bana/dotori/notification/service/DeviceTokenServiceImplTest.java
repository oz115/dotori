package com.bana.dotori.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.notification.dto.DeviceTokenRegisterRequest;
import com.bana.dotori.notification.dto.DeviceTokenRegisterResponse;
import com.bana.dotori.notification.entity.DeviceToken;
import com.bana.dotori.notification.entity.DeviceTokenPlatform;
import com.bana.dotori.notification.mapper.DeviceTokenMapper;
import com.bana.dotori.notification.repository.DeviceTokenRepository;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceImplTest {

	@Mock
	private DeviceTokenRepository deviceTokenRepository;

	@Mock
	private UserRepository userRepository;

	@Spy
	private DeviceTokenMapper deviceTokenMapper;

	@InjectMocks
	private DeviceTokenServiceImpl deviceTokenService;

	private User user(Integer id) {
		User user = User.builder()
			.loginId("dotori" + id)
			.password("encoded-password")
			.name("김도토")
			.email("dotori" + id + "@example.com")
			.role(UserRole.PARENT)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	@Test
	@DisplayName("처음 등록하는 토큰이면 새로 저장하고 응답을 반환한다")
	void register_newToken() {
		User user = user(1);
		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(deviceTokenRepository.findByFcmToken("fcm-token-abc")).willReturn(Optional.empty());
		given(deviceTokenRepository.save(any(DeviceToken.class))).willAnswer(invocation -> invocation.getArgument(0));

		DeviceTokenRegisterResponse response = deviceTokenService.register(1,
			new DeviceTokenRegisterRequest("fcm-token-abc", DeviceTokenPlatform.ANDROID));

		assertThat(response.platform()).isEqualTo(DeviceTokenPlatform.ANDROID);
		assertThat(response.registeredAt()).isNotNull();
		verify(deviceTokenRepository).save(any(DeviceToken.class));
	}

	@Test
	@DisplayName("이미 등록된 토큰이면 새로 저장하지 않고 소유자를 요청 사용자로 갈아치운다(upsert)")
	void register_existingToken_reassignsOwner() {
		User previousOwner = user(1);
		User newOwner = user(2);
		DeviceToken existing = DeviceToken.builder()
			.user(previousOwner)
			.fcmToken("fcm-token-abc")
			.platform(DeviceTokenPlatform.ANDROID)
			.build();
		given(userRepository.findById(2)).willReturn(Optional.of(newOwner));
		given(deviceTokenRepository.findByFcmToken("fcm-token-abc")).willReturn(Optional.of(existing));

		DeviceTokenRegisterResponse response = deviceTokenService.register(2,
			new DeviceTokenRegisterRequest("fcm-token-abc", DeviceTokenPlatform.IOS));

		assertThat(existing.getUser()).isEqualTo(newOwner);
		assertThat(existing.getPlatform()).isEqualTo(DeviceTokenPlatform.IOS);
		assertThat(response.platform()).isEqualTo(DeviceTokenPlatform.IOS);
		verify(deviceTokenRepository, never()).save(any(DeviceToken.class));
	}

	@Test
	@DisplayName("존재하지 않는 사용자면 NOT_FOUND 예외가 발생하고 토큰 조회는 하지 않는다")
	void register_userNotFound() {
		given(userRepository.findById(1)).willReturn(Optional.empty());

		assertThatThrownBy(() -> deviceTokenService.register(1,
			new DeviceTokenRegisterRequest("fcm-token-abc", DeviceTokenPlatform.ANDROID)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.NOT_FOUND);

		verify(deviceTokenRepository, never()).findByFcmToken(any());
	}

	@Test
	@DisplayName("본인 소유 토큰을 해제하면 삭제된다")
	void unregister_success() {
		DeviceToken existing = DeviceToken.builder()
			.user(user(1))
			.fcmToken("fcm-token-abc")
			.platform(DeviceTokenPlatform.ANDROID)
			.build();
		given(deviceTokenRepository.findByUser_IdAndFcmToken(1, "fcm-token-abc")).willReturn(Optional.of(existing));

		deviceTokenService.unregister(1, "fcm-token-abc");

		verify(deviceTokenRepository).delete(existing);
	}

	@Test
	@DisplayName("본인 소유가 아니거나 없는 토큰이면 DEVICE_TOKEN_NOT_FOUND 예외가 발생한다")
	void unregister_notFound() {
		given(deviceTokenRepository.findByUser_IdAndFcmToken(1, "fcm-token-abc")).willReturn(Optional.empty());

		assertThatThrownBy(() -> deviceTokenService.unregister(1, "fcm-token-abc"))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.DEVICE_TOKEN_NOT_FOUND);

		verify(deviceTokenRepository, never()).delete(any(DeviceToken.class));
	}

	@Test
	@DisplayName("fcmToken이 비어 있으면 INVALID_REQUEST 예외가 발생하고 조회하지 않는다")
	void unregister_blankFcmToken() {
		assertThatThrownBy(() -> deviceTokenService.unregister(1, " "))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_REQUEST);

		verify(deviceTokenRepository, never()).findByUser_IdAndFcmToken(any(), any());
	}
}
