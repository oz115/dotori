package com.bana.dotori.user.service;

/**
 * 한 도메인이 가진 "정리되지 않은 자산"이 사용자에게 남아 있는지 묻는다 (MYPAGE-05).
 *
 * <p>도메인마다 구현체 하나씩 둔다 (포인트 잔액, 환전 대기 요청, 예적금, 투자). 서비스는
 * 이 인터페이스가 아니라 {@link RemainingAssetPolicy} 로 묻는다 — 정책이 모든 구현체를 모아
 * 하나라도 남아 있으면 남은 것으로 본다.
 *
 * <p><b>새 자산 도메인을 만드는 사람에게</b>: 그 자산이 탈퇴·가족 해체·자녀 제외를 막아야 한다면
 * 이 인터페이스를 구현한 컴포넌트를 하나 추가하면 된다. 다른 코드는 고치지 않아도 된다.
 */
public interface RemainingAssetReader {

	boolean hasRemainingAsset(Integer userId);
}
