package com.bana.dotori.mock.bank;

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
 * <p>계좌 연동(ACC-01·02)이 완성되면 이 레코드는 계좌 도메인의 응답 DTO 로 대체된다.
 * 지금 형식을 맞춰 두는 이유는, 나중에 목업을 실제 API 로 갈아끼울 때
 * 충전 로직을 건드리지 않기 위해서다.
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
