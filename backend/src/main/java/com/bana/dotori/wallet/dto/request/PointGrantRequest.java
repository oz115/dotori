package com.bana.dotori.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * WLT-02 지급 요청 본문.
 *
 * <pre>
 * { "childId": 2, "amount": 5000, "description": "용돈" }
 * </pre>
 *
 * <p>명세상 {@code childId} 가 URL 경로와 본문 양쪽에 있다. 대상자가 두 군데에 적히는 구조는
 * 두 값이 어긋났을 때 엉뚱한 아이에게 포인트가 발행될 수 있어 위험하다.
 * 그래서 경로 값을 정본으로 삼고, 본문에 다른 값이 오면 조용히 무시하지 않고 400 으로 거부한다.
 * 명세를 고칠 수 있다면 본문에서 childId 를 빼는 편이 낫다.
 *
 * @param childId     지급 대상 자녀. 경로의 childId 와 같아야 한다
 * @param amount      지급 포인트
 * @param description 지급 사유. 거래내역 화면에 그대로 노출된다
 */
public record PointGrantRequest(
		Integer childId,

		@NotNull(message = "지급 포인트는 필수입니다")
		@Positive(message = "지급 포인트는 1 이상이어야 합니다")
		Integer amount,

		@Size(max = 255, message = "지급 사유는 255자를 넘을 수 없습니다")
		String description) {

	/**
	 * 본문에 childId 가 왔는데 경로와 다르면 어긋난 것으로 본다.
	 * 본문에 없으면(null) 경로 값을 그대로 쓴다.
	 */
	public boolean conflictsWith(Integer pathChildId) {
		return childId != null && !childId.equals(pathChildId);
	}
}
