package com.bana.dotori.wallet.dto.command;

import com.bana.dotori.wallet.entity.PointTransactionReason;

/**
 * 포인트 변경 한 건의 요청 값.
 *
 * <p>충전(WLT-01)·지급(WLT-02)·차감(WLT-05)이 모두 이 커맨드로 공통 서비스를 호출한다.
 * HTTP 요청 DTO 를 그대로 서비스에 넘기지 않는 이유는, 구매·환전·예적금처럼
 * 컨트롤러를 거치지 않고 내부에서 호출되는 경로가 있기 때문이다.
 *
 * @param childId         포인트가 변동될 대상 사용자
 * @param amount         변동 금액. 항상 양수이며 증감 방향은 서비스 메서드가 결정한다
 * @param reason         변동 사유
 * @param description    거래내역 화면에 노출될 설명
 * @param idempotencyKey 중복 방지 키. 중복 위험이 없는 내부 호출은 null 가능
 */
public record PointChangeCommand(
		Integer childId,
		int amount,
		PointTransactionReason reason,
		String description,
		String idempotencyKey) {

	public static PointChangeCommand of(Integer childId, int amount, PointTransactionReason reason,
			String description, String idempotencyKey) {
		return new PointChangeCommand(childId, amount, reason, description, idempotencyKey);
	}

	/**
	 * 멱등키 없이 호출하는 내부 거래용.
	 */
	public static PointChangeCommand withoutIdempotency(Integer childId, int amount, PointTransactionReason reason,
			String description) {
		return new PointChangeCommand(childId, amount, reason, description, null);
	}

	public boolean hasIdempotencyKey() {
		return idempotencyKey != null && !idempotencyKey.isBlank();
	}
}
