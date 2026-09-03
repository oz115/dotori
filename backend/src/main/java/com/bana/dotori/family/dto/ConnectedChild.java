package com.bana.dotori.family.dto;

import java.time.OffsetDateTime;

/**
 * 가족 모듈이 다른 모듈에 넘겨주는 자녀 요약.
 *
 * <p>마이페이지의 연결된 자녀 조회(MYPAGE-06)가 쓴다. 엔티티를 그대로 넘기면
 * 다른 모듈이 가족 도메인의 연관관계까지 따라갈 수 있어 최소 정보만 담는다.
 */
public record ConnectedChild(
	Integer userId,
	String name,
	OffsetDateTime joinedAt
) {
}
