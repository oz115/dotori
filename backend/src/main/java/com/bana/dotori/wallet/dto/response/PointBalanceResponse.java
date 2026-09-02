package com.bana.dotori.wallet.dto.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.bana.dotori.wallet.entity.PointWallet;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * WLT-04 보유 포인트 조회 응답.
 *
 * <pre>
 * {
 *   "childId": 2,
 *   "balance": 12000,
 *   "updatedAt": "2026-09-02T15:00:00+09:00"
 * }
 * </pre>
 *
 * <p>{@code updatedAt} 은 잔액이 마지막으로 바뀐 시각이다. 화면이 "방금 반영됨"을
 * 보여 주거나 응답을 캐시할 때 쓴다. 지갑이 아직 없으면 null 이다.
 */
public record PointBalanceResponse(
		Integer childId,
		int balance,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
		OffsetDateTime updatedAt) {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static PointBalanceResponse from(PointWallet wallet) {
		return new PointBalanceResponse(
				wallet.getUserId(),
				wallet.getBalance(),
				// DB 에서 읽어온 timestamptz 는 드라이버가 UTC 로 돌려주므로 KST 로 맞춘다.
				// 거래 응답(PointTransactionResponse)과 같은 이유다.
				wallet.getUpdatedAt().atZoneSameInstant(KST).toOffsetDateTime());
	}

	/**
	 * 지갑이 아직 없는 경우. 오류가 아니라 잔액 0으로 응답한다.
	 *
	 * <p>가족 연결이 끝나면 지갑이 잔액 0으로 만들어지므로 정상 흐름에서는 나오지 않는다.
	 * 그래도 지갑 생성 전에 화면이 먼저 열리는 경우가 있어 방어해 둔다.
	 */
	public static PointBalanceResponse empty(Integer childId) {
		return new PointBalanceResponse(childId, 0, null);
	}
}
