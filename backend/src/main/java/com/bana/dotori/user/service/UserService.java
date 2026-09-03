package com.bana.dotori.user.service;

import com.bana.dotori.user.dto.ConnectedChildrenResponse;
import com.bana.dotori.user.dto.PasswordChangeRequest;
import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
import com.bana.dotori.user.dto.UserInfoResponse;
import com.bana.dotori.user.dto.UserUpdateRequest;
import com.bana.dotori.user.dto.UserUpdateResponse;

public interface UserService {

	SignupResponse signup(SignupRequest request);

	/**
	 * MYPAGE-01 회원 정보 조회. 본인 계정만 조회할 수 있다.
	 *
	 * @param loginUserId  인증된 요청자
	 * @param targetUserId 경로에서 지정한 조회 대상
	 */
	UserInfoResponse getUserInfo(Integer loginUserId, Integer targetUserId);

	/**
	 * MYPAGE-02 회원 정보 수정. 본인 계정만, 전달된 필드만 바꾼다.
	 *
	 * @param loginUserId  인증된 요청자
	 * @param targetUserId 경로에서 지정한 수정 대상
	 */
	UserUpdateResponse updateUserInfo(Integer loginUserId, Integer targetUserId, UserUpdateRequest request);

	/**
	 * MYPAGE-04 비밀번호 변경. 본인 계정만, 현재 비밀번호를 확인한 뒤 바꾼다.
	 *
	 * <p>성공하면 그 사용자의 기존 Refresh Token 을 모두 폐기한다.
	 *
	 * @param loginUserId  인증된 요청자
	 * @param targetUserId 경로에서 지정한 변경 대상
	 */
	void changePassword(Integer loginUserId, Integer targetUserId, PasswordChangeRequest request);

	/**
	 * MYPAGE-06 연결된 자녀 조회. 본인 계정의, 부모 역할일 때만 볼 수 있다.
	 *
	 * @param loginUserId  인증된 요청자
	 * @param targetUserId 경로에서 지정한 조회 대상
	 */
	ConnectedChildrenResponse getConnectedChildren(Integer loginUserId, Integer targetUserId);
}
