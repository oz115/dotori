package com.bana.dotori.wallet.entity;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * WLT-07 포인트 환전 요청 한 건.
 *
 * <p>자녀가 요청하면 포인트는 그 즉시 차감되고({@code PointDeductionService}, 이 요청과는
 * 별도 트랜잭션), 이 행은 PENDING 으로 남아 부모 승인(또는 거절, 자녀의 취소)을 기다린다.
 * 승인되면 부모→자녀 실제 송금이 일어나고 SUCCEED 로 바뀐다. 송금이 실패하면 차감했던
 * 포인트를 환급하고 FAILED 로 남긴다. 자녀가 직접 취소하거나 부모가 거절하면 각각
 * CANCELLED/DECLINED 로 남기고 마찬가지로 환급한다.
 */
@Entity
@Getter
@Table(name = "point_cashout_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointCashoutRequest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cashout_request_id")
	private Integer cashoutRequestId;

	@Column(name = "family_id", nullable = false, updatable = false)
	private Integer familyId;

	@Column(name = "child_id", nullable = false, updatable = false)
	private Integer childId;

	/**
	 * 요청 시점에 정해진, 실제로 송금을 보낼 부모. 승인 자체는 그 가족의 어느 부모나 할 수
	 * 있지만, 돈은 항상 이 부모의 계좌에서 나간다.
	 */
	@Column(name = "parent_id", nullable = false, updatable = false)
	private Integer parentId;

	@Column(name = "point_amount", nullable = false, updatable = false)
	private int pointAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private PointCashoutRequestStatus status;

	@Column(name = "requested_at", nullable = false, updatable = false)
	private OffsetDateTime requestedAt;

	@Column(name = "processed_at")
	private OffsetDateTime processedAt;

	private PointCashoutRequest(Integer familyId, Integer childId, Integer parentId, int pointAmount) {
		this.familyId = familyId;
		this.childId = childId;
		this.parentId = parentId;
		this.pointAmount = pointAmount;
		this.status = PointCashoutRequestStatus.PENDING;
		this.requestedAt = OffsetDateTime.now(KST);
	}

	/**
	 * 차감이 이미 끝난 뒤, PENDING 상태의 요청 행을 새로 만든다.
	 */
	public static PointCashoutRequest pending(Integer familyId, Integer childId, Integer parentId, int pointAmount) {
		return new PointCashoutRequest(familyId, childId, parentId, pointAmount);
	}

	/**
	 * 송금이 끝나 승인 처리한다.
	 *
	 * @throws BusinessException CASHOUT_REQUEST_ALREADY_PROCESSED PENDING 이 아닌 경우
	 */
	public void succeed() {
		requirePending();
		this.status = PointCashoutRequestStatus.SUCCEED;
		this.processedAt = OffsetDateTime.now(KST);
	}

	/**
	 * 승인을 시도했지만 송금이 실패해, 차감했던 포인트를 환급한 뒤 종료 상태로 남긴다.
	 *
	 * @throws BusinessException CASHOUT_REQUEST_ALREADY_PROCESSED PENDING 이 아닌 경우
	 */
	public void fail() {
		requirePending();
		this.status = PointCashoutRequestStatus.FAILED;
		this.processedAt = OffsetDateTime.now(KST);
	}

	/**
	 * 자녀가 자기 요청을 직접 취소한다. 차감했던 포인트는 호출자가 별도로 환급한다.
	 *
	 * @throws BusinessException CASHOUT_REQUEST_ALREADY_PROCESSED PENDING 이 아닌 경우
	 */
	public void cancel() {
		requirePending();
		this.status = PointCashoutRequestStatus.CANCELLED;
		this.processedAt = OffsetDateTime.now(KST);
	}

	/**
	 * 부모가 요청을 거절한다. 차감했던 포인트는 호출자가 별도로 환급한다.
	 *
	 * @throws BusinessException CASHOUT_REQUEST_ALREADY_PROCESSED PENDING 이 아닌 경우
	 */
	public void decline() {
		requirePending();
		this.status = PointCashoutRequestStatus.DECLINED;
		this.processedAt = OffsetDateTime.now(KST);
	}

	private void requirePending() {
		if (this.status != PointCashoutRequestStatus.PENDING) {
			throw new BusinessException(ErrorCode.CASHOUT_REQUEST_ALREADY_PROCESSED,
					"이미 처리된 환전 요청입니다. cashoutRequestId=" + cashoutRequestId + ", status=" + status);
		}
	}
}
