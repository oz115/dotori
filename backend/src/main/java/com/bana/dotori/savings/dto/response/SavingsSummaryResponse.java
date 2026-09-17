package com.bana.dotori.savings.dto.response;

import java.time.OffsetDateTime;

/**
 * 부모 홈 "자녀 예적금" 카드 한 장을 채우는 요약. 가입 건 목록을 통째로 내려 클라이언트가
 * 집계하게 두면, 끝난 건(COMPLETED·CANCELLED)까지 실어 보내면서 정작 화면이 쓰는 값은
 * 네댓 개뿐이라 한 번의 호출로 끝내려고 따로 둔다.
 *
 * <p>집계 대상은 <b>진행 중(ACTIVE)과 수령 대기(MATURED) 건뿐</b>이다. 이미 수령하거나 해지한
 * 건은 지갑으로 빠져나가 아이의 예적금 잔액이 아니다.
 *
 * @param activeCount          납입·정산이 돌고 있는 가입 건수
 * @param maturedCount         만기에 닿았지만 아이가 아직 수령하지 않은 건수. 자동 지급이 아니라
 *                             아이가 눌러야 지급되는 구조라, 부모가 챙겨 볼 수 있게 따로 센다
 * @param totalCurrentAmount   위 건들에 납입된 원금 누계 (이자 제외)
 * @param totalAccruedInterest 위 건들에 쌓인 이자 누계. 보통예금 이자는 매주 지갑으로 바로
 *                             나가므로 여기 잡히지 않는다 — 이 값은 "아직 못 받은 이자"다
 * @param totalAmount          원금 + 이자. 지금 전부 만기가 온다면 받게 될 금액이다
 * @param nearestMaturityAt    진행 중인 건 가운데 가장 빨리 오는 만기 시각.
 *                             만기가 있는 건이 하나도 없으면(보통예금뿐이면) null
 */
public record SavingsSummaryResponse(
	int activeCount,
	int maturedCount,
	int totalCurrentAmount,
	int totalAccruedInterest,
	int totalAmount,
	OffsetDateTime nearestMaturityAt
) {
}
