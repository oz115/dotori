package com.bana.dotori.user.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.bana.dotori.auth.service.AuthService;
import com.bana.dotori.auth.service.EmailVerificationCodeManager;
import com.bana.dotori.family.service.FamilyService;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.util.PasswordPolicy;
import com.bana.dotori.user.dto.ConnectedChildrenResponse;
import com.bana.dotori.user.dto.PasswordChangeRequest;
import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
import com.bana.dotori.user.dto.UserInfoResponse;
import com.bana.dotori.user.dto.UserUpdateRequest;
import com.bana.dotori.user.dto.UserUpdateResponse;
import com.bana.dotori.user.dto.UserWithdrawRequest;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.mapper.UserMapper;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final AuthService authService;
	private final FamilyService familyService;
	private final MyPageAccessPolicy accessPolicy;
	private final RemainingAssetPolicy remainingAssetPolicy;
	private final EmailVerificationCodeManager emailVerificationCodeManager;

	@Override
	@Transactional
	public SignupResponse signup(SignupRequest request) {
		SignupRequest normalized = normalizeEmail(request);
		// 이메일 인증(AUTH-07)을 통과한 요청인지부터 확인한다. 인증 안 된 이메일로는 가입할 수 없다.
		emailVerificationCodeManager.requireValidToken(normalized.email(), normalized.verificationToken());
		if (userRepository.existsByLoginIdAndDeletedAtIsNull(normalized.loginId())) {
			throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
		}
		if (userRepository.existsByEmailAndDeletedAtIsNull(normalized.email())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		User user = userMapper.toEntity(normalized, passwordEncoder.encode(normalized.password()));

		try {
			SignupResponse response = userMapper.toSignupResponse(userRepository.save(user));
			consumeVerificationTokenAfterCommit(normalized.email());
			return response;
		} catch (DataIntegrityViolationException e) {
			// 중복 체크와 save 사이에 동시 요청이 끼어들면 unique 제약 위반이 여기로 온다 → 409로 변환
			throw new BusinessException(resolveDuplicateError(e));
		}
	}

	/**
	 * verificationToken(1회용) 소모는 커밋이 확정된 뒤에만 한다. 커밋 전에 지우면
	 * 커밋 실패(롤백) 시 가입은 안 됐는데 토큰만 사라져 인증을 처음부터 다시 해야 한다.
	 * (비밀번호 재설정의 토큰 소모와 같은 규칙 — AuthServiceImpl.resetPassword)
	 */
	private void consumeVerificationTokenAfterCommit(String email) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					emailVerificationCodeManager.deleteToken(email);
				}
			});
		} else {
			emailVerificationCodeManager.deleteToken(email);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void checkLoginIdAvailable(String loginId) {
		if (userRepository.existsByLoginIdAndDeletedAtIsNull(loginId)) {
			throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
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
		PasswordPolicy.validate(request.newPassword());

		user.changePassword(passwordEncoder.encode(request.newPassword()));

		// 비밀번호를 바꾸는 이유가 유출일 수 있다. 기존 Refresh Token 을 남겨두면
		// 훔쳐간 쪽이 계속 Access Token 을 재발급받으므로 전부 끊는다.
		authService.revokeAllRefreshTokens(targetUserId);
	}

	@Override
	@Transactional(readOnly = true)
	public ConnectedChildrenResponse getConnectedChildren(Integer loginUserId, Integer targetUserId) {
		accessPolicy.requireSelf(loginUserId, targetUserId);
		accessPolicy.requireParent(findUser(targetUserId));

		// 가족이 없으면 빈 목록이 온다. 자녀가 없는 것과 구분하지 않고 그대로 빈 배열로 내려준다.
		return userMapper.toConnectedChildrenResponse(familyService.findConnectedChildren(targetUserId));
	}

	@Override
	@Transactional
	public void withdraw(Integer loginUserId, Integer targetUserId, UserWithdrawRequest request) {
		accessPolicy.requireSelf(loginUserId, targetUserId);

		User user = findUser(targetUserId);

		// 되돌릴 수 없는 동작이라 본인 확인이 먼저다.
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
		}
		// 부모가 자녀를 남긴 채 나가면 자녀들은 부모 없는 가족에 갇힌다.
		// 자녀를 먼저 내보낸 뒤(FAM-05) 탈퇴해야 한다.
		if (user.getRole() == UserRole.PARENT && !familyService.findConnectedChildren(targetUserId).isEmpty()) {
			throw new BusinessException(ErrorCode.FAMILY_HAS_CHILDREN,
				"가족에 남은 자녀를 먼저 내보낸 뒤 탈퇴할 수 있습니다. userId=" + targetUserId);
		}
		// 자산(도토리·환전 대기·예적금·투자)을 남긴 채 탈퇴하면 그것을 되찾을 방법이 없다.
		if (remainingAssetPolicy.hasRemainingAsset(targetUserId)) {
			throw new BusinessException(ErrorCode.HAS_ACTIVE_ASSETS,
				"잔여 자산을 정리한 뒤 탈퇴할 수 있습니다. userId=" + targetUserId);
		}

		user.withdraw();

		// 탈퇴한 계정의 토큰이 살아 있으면 계정이 없는 상태로 API 를 계속 호출할 수 있다.
		authService.revokeAllRefreshTokens(targetUserId);
	}

	private User findUser(Integer userId) {
		// 탈퇴한 계정은 없는 것으로 본다. 로그인이 막혀 있어 정상 흐름에서는 닿지 않지만,
		// 토큰이 남은 상태로 들어오는 경우까지 여기서 끊는다.
		return userRepository.findById(userId)
			.filter(found -> found.getDeletedAt() == null)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다. userId=" + userId));
	}

	private SignupRequest normalizeEmail(SignupRequest request) {
		return new SignupRequest(request.loginId(), request.password(), request.name(),
			request.email().toLowerCase(Locale.ROOT), request.role(), request.verificationToken());
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
