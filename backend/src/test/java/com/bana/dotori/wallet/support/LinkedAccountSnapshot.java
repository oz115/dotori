package com.bana.dotori.wallet.support;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 연동 계좌 한 건. API 명세 ACC-02(계좌 연동) · ACC-03(연동 계좌 상세 + 잔액 조회)의
 * 응답 형식을 그대로 따른다.
 *
 * <pre>
 * {
 *   "accountId": 1,
 *   "bankCode": "088",
 *   "bankName": "신한은행",
 *   "accountNumber": "110-123-456789",
 *   "accountName": "김도토",
 *   "balance": 500000,
 *   "linkedAt": "2026-08-28T09:00:00+09:00",
 *   "updatedAt": "2026-08-28T12:00:00+09:00"
 * }
 * </pre>
 *
 * @param balance 현재 잔액(원). 포인트가 아니라 실제 돈이다
 */
public record LinkedAccountSnapshot(
		Integer accountId,
		String bankCode,
		String bankName,
		String accountNumber,
		String accountName,
		int balance,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
		OffsetDateTime linkedAt,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
		OffsetDateTime updatedAt) {

	public LinkedAccountSnapshot withBalance(int newBalance, OffsetDateTime updatedAt) {
		return new LinkedAccountSnapshot(accountId, bankCode, bankName, accountNumber, accountName,
				newBalance, linkedAt, updatedAt);
	}
}
