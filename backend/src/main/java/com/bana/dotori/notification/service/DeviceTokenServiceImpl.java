package com.bana.dotori.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.notification.dto.DeviceTokenRegisterRequest;
import com.bana.dotori.notification.dto.DeviceTokenRegisterResponse;
import com.bana.dotori.notification.entity.DeviceToken;
import com.bana.dotori.notification.mapper.DeviceTokenMapper;
import com.bana.dotori.notification.repository.DeviceTokenRepository;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {

	private final DeviceTokenRepository deviceTokenRepository;
	private final UserRepository userRepository;
	private final DeviceTokenMapper deviceTokenMapper;

	@Override
	@Transactional
	public DeviceTokenRegisterResponse register(Integer userId, DeviceTokenRegisterRequest request) {
		User user = findUser(userId);

		// 동일 토큰의 동시 등록(같은 기기의 중복 호출 등)이 겹치면 unique 제약 위반으로 500이 될 수 있다 —
		// 실사용 시나리오상 발생 빈도가 낮아 지금은 재시도 로직을 두지 않는다.
		DeviceToken deviceToken = deviceTokenRepository.findByFcmToken(request.fcmToken())
			.map(existing -> {
				existing.reassignTo(user, request.platform());
				return existing;
			})
			.orElseGet(() -> deviceTokenRepository.save(deviceTokenMapper.toEntity(request, user)));

		return deviceTokenMapper.toRegisterResponse(deviceToken);
	}

	private User findUser(Integer userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다. userId=" + userId));
	}
}
