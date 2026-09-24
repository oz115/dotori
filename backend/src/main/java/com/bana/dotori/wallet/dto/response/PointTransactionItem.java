package com.bana.dotori.wallet.dto.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * WLT-06 거래내역 목록의 한 줄.
 *
 * <pre>
 * {
 *   "pointTransactionId": 101,
 *   "transactionType": "CHARGE",
 *   "transactionDirection": "IN",
 *   "amount": 1000,
 *   "balanceAfter": 6000,
 *   "description": "포인트 충전",
 *   "createdAt": "2026-09-02T15:00:00+09:00"
 * }
 * </pre>
 *
 * <p>충전·지급 응답({@link PointTransactionResponse})과 필드가 겹치지만 따로 두었다.
 * 목록 화면은 {@code description} 과 입출금 방향이 필요하고, 그 두 개를 저쪽에 더하면
 * 이미 FE 와 맞춰 둔 충전·지급 응답 형식이 바뀐다.
 *
 * @param transactionDirection 잔액이 늘었는지 줄었는지. DB 에 저장하지 않고 사유에서 계산한다
 */
public record PointTransactionItem(
		Integer pointTransactionId,
		PointTransactionReason transactionType,
		String transactionDirection,
		int amount,
		int balanceAfter,
		String description,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
		OffsetDateTime createdAt) {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/** 명세(WLT-06)가 요구하는 표기. */
	private static final String IN = "IN";
	private static final String OUT = "OUT";

	public static PointTransactionItem from(PointTransaction transaction) {
		return new PointTransactionItem(
				transaction.getPointTransactionId(),
				transaction.getReason(),
				resolveDirection(transaction.getReason()),
				transaction.getAmount(),
				transaction.getBalanceAfter(),
				transaction.getDescription(),
				// DB 의 timestamptz 는 드라이버가 UTC 로 돌려주므로 KST 로 맞춘다.
				transaction.getCreatedAt().atZoneSameInstant(KST).toOffsetDateTime());
	}

	/**
	 * 사유에서 입출금 방향을 계산한다.
	 *
	 * <p>방향은 컬럼으로 저장하지 않는다(ARCHITECTURE.md 참고). 사유가 한 방향만
	 * 허용하면 그대로 정해지고, 예적금·투자처럼 양방향이면 앞 거래와의 잔액 차이로 판단한다.
	 * 목록은 거래를 한 줄씩 내보내므로 앞 거래를 알 수 없어, 양방향 사유는
	 * {@code balanceAfter} 만으로는 못 정한다. 그래서 양방향은 방향을 비워 둔다.
	 */
	private static String resolveDirection(PointTransactionReason reason) {
		if (reason.isDepositAllowed() && reason.isWithdrawalAllowed()) {
			// SAVINGS·INVESTMENT. 아직 해당 도메인이 없어 실제로는 나오지 않는다.
			// 그쪽이 붙을 때 거래에 방향을 남길지 정해야 한다.
			return null;
		}
		return reason.isDepositAllowed() ? IN : OUT;
	}
}
