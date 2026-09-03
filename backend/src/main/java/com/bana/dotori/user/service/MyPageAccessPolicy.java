package com.bana.dotori.user.service;

import org.springframework.stereotype.Component;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;

/**
 * 마이페이지(MYPAGE-01·02·04·06)가 공통으로 쓰는 접근 판정.
 *
 * <p>네 API 가 모두 "경로의 userId 가 나인가"를 묻는다. 판정이 흩어지면 한 곳만
 * 고쳐지는 일이 생기므로 여기로 모은다.
 */
@Component
public class MyPageAccessPolicy {

	/**
	 * 경로가 가리키는 사용자가 요청자 본인인지 확인한다.
	 *
	 * <p>회원을 찾기 전에 부른다. 순서를 뒤집으면 없는 id 는 404, 남의 id 는 403 이 되어
	 * 응답 코드만으로 다른 계정의 존재 여부를 알아낼 수 있다.
	 *
	 * @param loginUserId  인증 주체. 시큐리티가 막아 주므로 정상 흐름에서는 null 이 아니다
	 * @param targetUserId 경로 변수로 들어온 대상
	 */
	public void requireSelf(Integer loginUserId, Integer targetUserId) {
		// 인증이 없는데 여기까지 왔다면 권한 부족(403)이 아니라 인증 실패(401)다.
		// 지금은 시큐리티 설정이 먼저 막지만, 경로가 permitAll 로 바뀌면 여기가 유일한 방어선이 된다.
		if (loginUserId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증 주체가 없습니다. targetUserId=" + targetUserId);
		}
		if (!loginUserId.equals(targetUserId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
				"본인의 회원 정보만 다룰 수 있습니다. loginUserId=" + loginUserId + ", targetUserId=" + targetUserId);
		}
	}

	/**
	 * 부모 계정인지 확인한다. 연결된 자녀 조회(MYPAGE-06)처럼 부모 전용 기능이 쓴다.
	 */
	public void requireParent(User user) {
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
				"부모 계정만 접근할 수 있습니다. userId=" + user.getId() + ", role=" + user.getRole());
		}
	}
}
