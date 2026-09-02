package com.bana.dotori.wallet.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bana.dotori.wallet.entity.PointTransactionReason;

/**
 * DTO 계층 - 응답의 시각 표기를 검증한다.
 *
 * <p>명세는 createdAt 을 +09:00 오프셋으로 정의한다. 방금 만든 거래는 그렇게 나가지만,
 * 멱등키로 DB 에서 다시 읽어온 거래는 timestamptz 특성상 드라이버가 UTC 로 돌려주기 때문에
 * 보정이 필요하다. 실제 서버에서 같은 충전 요청의 응답 두 개가 서로 다른 오프셋으로 나가는 것을
 * 확인하고 추가한 테스트다.
 */
@DisplayName("PointTransactionResponse 시각 표기")
class PointTransactionResponseTest {

	@Test
	@DisplayName("시각_DB에서_다시_읽은_거래도_KST_오프셋으로_나간다")
	void 시각_DB에서_다시_읽은_거래도_KST_오프셋으로_나간다() {
		OffsetDateTime utc = OffsetDateTime.parse("2026-09-02T01:47:48Z");
		PointChangeResult result = new PointChangeResult(1, 10, PointTransactionReason.CHARGE,
				10_000, 10_000, utc, true);

		PointTransactionResponse response = PointTransactionResponse.from(result);

		assertThat(response.createdAt().getOffset())
				.as("표기가 갈리면 클라이언트가 9시간 차이로 오해한다")
				.isEqualTo(ZoneOffset.ofHours(9));
		assertThat(response.createdAt().toInstant())
				.as("표기만 바꾸고 시각 자체는 그대로여야 한다")
				.isEqualTo(utc.toInstant());
	}

	@Test
	@DisplayName("시각_이미_KST인_거래는_그대로_유지된다")
	void 시각_이미_KST인_거래는_그대로_유지된다() {
		OffsetDateTime kst = OffsetDateTime.parse("2026-09-02T10:47:48+09:00");
		PointChangeResult result = new PointChangeResult(1, 10, PointTransactionReason.CHARGE,
				10_000, 10_000, kst, false);

		assertThat(PointTransactionResponse.from(result).createdAt()).isEqualTo(kst);
	}
}
