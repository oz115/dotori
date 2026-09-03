package com.bana.dotori.user.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
import com.bana.dotori.user.dto.UserInfoResponse;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.mapper.UserMapper;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public SignupResponse signup(SignupRequest request) {
		SignupRequest normalized = normalizeEmail(request);
		if (userRepository.existsByLoginId(normalized.loginId())) {
			throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
		}
		if (userRepository.existsByEmail(normalized.email())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		User user = userMapper.toEntity(normalized, passwordEncoder.encode(normalized.password()));

		try {
			return userMapper.toSignupResponse(userRepository.save(user));
		} catch (DataIntegrityViolationException e) {
			// 중복 체크와 save 사이에 동시 요청이 끼어들면 unique 제약 위반이 여기로 온다 → 409로 변환
			throw new BusinessException(resolveDuplicateError(e));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public UserInfoResponse getUserInfo(Integer loginUserId, Integer targetUserId) {
		// 본인 확인을 조회보다 먼저 한다. 순서를 뒤집으면 없는 id 는 404, 남의 id 는 403 이 되어
		// 응답 코드만으로 다른 계정의 존재 여부를 알아낼 수 있다.
		if (!targetUserId.equals(loginUserId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
				"본인의 회원 정보만 조회할 수 있습니다. loginUserId=" + loginUserId + ", targetUserId=" + targetUserId);
		}

		User user = userRepository.findById(targetUserId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다. userId=" + targetUserId));

		return userMapper.toUserInfoResponse(user);
	}

	private SignupRequest normalizeEmail(SignupRequest request) {
		return new SignupRequest(request.loginId(), request.password(), request.name(),
			request.email().toLowerCase(Locale.ROOT), request.role());
	}

	private ErrorCode resolveDuplicateError(DataIntegrityViolationException e) {
		String message = String.valueOf(e.getMostSpecificCause().getMessage());
		if (message.contains("login_id")) {
			return ErrorCode.DUPLICATE_LOGIN_ID;
		}
		if (message.contains("email")) {
			return ErrorCode.DUPLICATE_EMAIL;
		}
		throw e;
	}
}
