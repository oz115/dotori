package com.bana.dotori.user.service;

import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
import com.bana.dotori.user.dto.UserInfoResponse;

public interface UserService {

	SignupResponse signup(SignupRequest request);

	/**
	 * MYPAGE-01 회원 정보 조회. 본인 계정만 조회할 수 있다.
	 *
	 * @param loginUserId  인증된 요청자
	 * @param targetUserId 경로에서 지정한 조회 대상
	 */
	UserInfoResponse getUserInfo(Integer loginUserId, Integer targetUserId);
}
