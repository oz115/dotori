package com.bana.dotori.user.service;

import com.bana.dotori.user.dto.ConnectedChildrenResponse;
import com.bana.dotori.user.dto.PasswordChangeRequest;
import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
import com.bana.dotori.user.dto.UserInfoResponse;
import com.bana.dotori.user.dto.UserUpdateRequest;
import com.bana.dotori.user.dto.UserUpdateResponse;
import com.bana.dotori.user.dto.UserWithdrawRequest;

public interface UserService {

	SignupResponse signup(SignupRequest request);

	/**
	 * 아이디 사용 가능 여부 확인 (회원가입 화면의 중복 체크 버튼).
	 *
	 * <p>사용 중이면 DUPLICATE_LOGIN_ID 를 던진다. 확인과 가입 사이에 다른 가입이
	 * 끼어들 수 있으므로 signup 의 최종 중복 검사를 대체하지 않는다.
	 */
	void checkLoginIdAvailable(String loginId);

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

	/**
	 * MYPAGE-05 회원 탈퇴. 본인 계정만, 비밀번호를 확인한 뒤 처리한다.
	 *
	 * <p>잔여 자산이 있으면 거부하고, 성공하면 Refresh Token 을 모두 폐기한다.
	 *
	 * @param loginUserId  인증된 요청자
	 * @param targetUserId 경로에서 지정한 탈퇴 대상
	 */
	void withdraw(Integer loginUserId, Integer targetUserId, UserWithdrawRequest request);
}
