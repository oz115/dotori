package com.bana.dotori.family.dto;

/**
 * 가족 상태 조회 응답. familyId 는 소속 가족이 없으면 null 이다.
 *
 * <p>초대 코드가 만료돼 온보딩으로 되돌아온 부모가 가족 생성(ALREADY_IN_FAMILY 로 막힘)이
 * 아니라 이 id 로 초대 코드 재발급(POST /families/{familyId}/invitations)으로 이어 가게 한다.
 */
public record FamilyStatusResponse(
	boolean hasFamily,
	Integer familyId
) {
}
