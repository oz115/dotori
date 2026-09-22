package com.bana.dotori.family.event;

/**
 * 가족 생성 직후 발행된다. 생성 요청이 기본 용돈·보상 한도까지 받으므로 이 시점이 곧
 * 온보딩 완료다. 투자 도메인이 받아서 가격 이력 백필(가입 당일 차트)을 트리거한다.
 */
public record FamilyOnboardedEvent(Integer familyId) {
}
