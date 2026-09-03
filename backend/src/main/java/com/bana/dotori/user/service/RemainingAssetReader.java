package com.bana.dotori.user.service;

/**
 * 탈퇴를 막아야 하는 잔여 자산이 있는지 묻는다 (MYPAGE-05).
 *
 * <p>명세의 HAS_ACTIVE_ASSETS 는 포인트·예적금·투자를 함께 가리키는데, 지금 존재하는
 * 도메인은 포인트뿐이라 구현도 포인트만 본다.
 *
 * <p><b>예적금·투자 도메인을 만드는 사람에게</b>: 그 자산도 탈퇴를 막아야 한다면
 * 이 인터페이스의 구현에 조건을 더하면 된다. 여기 한 곳만 고치면 탈퇴 API 에 그대로 반영된다.
 * 검사를 각 서비스에 흩어 두면 빠뜨리기 쉬워서 질문을 하나로 모아 둔다.
 */
public interface RemainingAssetReader {

	boolean hasRemainingAsset(Integer userId);
}
