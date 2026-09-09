package com.bana.dotori.wallet.dto.response;

import java.time.OffsetDateTime;

import com.bana.dotori.wallet.entity.PointCashoutRequest;
import com.bana.dotori.wallet.entity.PointCashoutRequestStatus;

/**
 * 환전 요청 처리 결과. {@code PointChangeResult}와 같은 이유로, 서비스 밖으로는
 * JPA 엔티티를 그대로 내보내지 않고 이 record 로 감싼다.
 */
public record PointCashoutRequestResult(
		Integer cashoutRequestId,
		Integer familyId,
		Integer childId,
		Integer parentId,
		int pointAmount,
		PointCashoutRequestStatus status,
		OffsetDateTime requestedAt,
		OffsetDateTime processedAt) {

	public static PointCashoutRequestResult from(PointCashoutRequest request) {
		return new PointCashoutRequestResult(
				request.getCashoutRequestId(),
				request.getFamilyId(),
				request.getChildId(),
				request.getParentId(),
				request.getPointAmount(),
				request.getStatus(),
				request.getRequestedAt(),
				request.getProcessedAt());
	}
}
