package com.bana.dotori.user.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.auth.service.AuthService;
import com.bana.dotori.family.service.FamilyService;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.dto.ConnectedChildrenResponse;
import com.bana.dotori.user.dto.PasswordChangeRequest;
import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
import com.bana.dotori.user.dto.UserInfoResponse;
import com.bana.dotori.user.dto.UserUpdateRequest;
import com.bana.dotori.user.dto.UserUpdateResponse;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.mapper.UserMapper;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	// 회원가입(SignupRequest)의 @Size 와 같은 정책이다. 한쪽만 바뀌면 가입과 변경의 기준이 갈린다.
	private static final int PASSWORD_MIN_LENGTH = 8;
	private static final int PASSWORD_MAX_LENGTH = 64;

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final AuthService authService;
	private final FamilyService familyService;
	private final MyPageAccessPolicy accessPolicy;

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
		accessPolicy.requireSelf(loginUserId, targetUserId);

		return userMapper.toUserInfoResponse(findUser(targetUserId));
	}

	@Override
	@Transactional
	public UserUpdateResponse updateUserInfo(Integer loginUserId, Integer targetUserId, UserUpdateRequest request) {
		accessPolicy.requireSelf(loginUserId, targetUserId);

		User user = findUser(targetUserId);

		// 부분 수정이라 안 온 필드는 건드리지 않는다. 값이 온 필드만 바꾼다.
		if (request.name() != null) {
			user.changeName(request.name());
		}

		// updatedAt 은 @UpdateTimestamp 가 flush 시점에 채운다.
		// 먼저 flush 하지 않으면 응답에 갱신 전 값이 담긴다.
		return userMapper.toUserUpdateResponse(userRepository.saveAndFlush(user));
	}

	@Override
	@Transactional
	public void changePassword(Integer loginUserId, Integer targetUserId, PasswordChangeRequest request) {
		accessPolicy.requireSelf(loginUserId, targetUserId);

		User user = findUser(targetUserId);

		// 현재 비밀번호부터 확인한다. 정책 검사를 먼저 하면 응답이 갈려서
		// 새 비밀번호 형식만 바꿔가며 현재 비밀번호를 떠보는 여지가 생긴다.
		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
		}
		validatePasswordPolicy(request.newPassword());

		user.changePassword(passwordEncoder.encode(request.newPassword()));

		// 비밀번호를 바꾸는 이유가 유출일 수 있다. 기존 Refresh Token 을 남겨두면
		// 훔쳐간 쪽이 계속 Access Token 을 재발급받으므로 전부 끊는다.
		authService.revokeAllRefreshTokens(targetUserId);
	}

	private void validatePasswordPolicy(String newPassword) {
		if (newPassword == null || newPassword.isBlank()
			|| newPassword.length() < PASSWORD_MIN_LENGTH
			|| newPassword.length() > PASSWORD_MAX_LENGTH) {

			throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT,
				"비밀번호는 공백 없이 " + PASSWORD_MIN_LENGTH + "자 이상 " + PASSWORD_MAX_LENGTH + "자 이하여야 합니다");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ConnectedChildrenResponse getConnectedChildren(Integer loginUserId, Integer targetUserId) {
		accessPolicy.requireSelf(loginUserId, targetUserId);
		accessPolicy.requireParent(findUser(targetUserId));

		// 가족이 없으면 빈 목록이 온다. 자녀가 없는 것과 구분하지 않고 그대로 빈 배열로 내려준다.
		return userMapper.toConnectedChildrenResponse(familyService.findConnectedChildren(targetUserId));
	}

	private User findUser(Integer userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다. userId=" + userId));
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
