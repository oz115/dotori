package com.bana.dotori.global.util;

/**
 * 팀 환전 정책. 현금 10원 = 포인트 1점.
 *
 * <p>부모·아이가 화면에서 다루는 금액은 원(₩)이고 내부 계산·저장은 포인트다.
 * 환율이 도메인마다 흩어지면 서로 다른 비율로 환산하는 사고가 나므로
 * 비밀번호 정책({@code PasswordPolicy})과 같은 이유로 한 곳에 둔다.
 * 포인트 충전(지갑)과 가족 온보딩 설정이 함께 쓴다.
 */
public final class PointExchangePolicy {

	/** 포인트 1점을 만드는 현금(원). */
	public static final int KRW_PER_POINT = 10;

	private PointExchangePolicy() {
	}

	/**
	 * 포인트로 나누어떨어지는 원 단위 금액인지 확인한다.
	 * 어중간한 금액을 허용하면 버림으로 잔돈이 사라지므로 호출부가 이 검증으로 먼저 거른다.
	 */
	public static boolean isWonUnit(int won) {
		return won % KRW_PER_POINT == 0;
	}

	/** 원을 포인트로 환산한다. {@link #isWonUnit} 검증을 통과한 금액만 넘긴다. */
	public static int toPoints(int won) {
		return won / KRW_PER_POINT;
	}
}
