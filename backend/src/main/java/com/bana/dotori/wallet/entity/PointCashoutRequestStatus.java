package com.bana.dotori.wallet.entity;

/**
 * 환전 요청의 처리 상태.
 *
 * <p>{@code point_cashout_requests.status} 컬럼에 저장한다. 운영 DB에서는 이 컬럼이
 * 네이티브 ENUM({@code point_cashout_requests_status})이므로 여기 상수 이름이
 * 그 타입의 라벨과 정확히 같아야 한다.
 */
public enum PointCashoutRequestStatus {

	/** 부모 승인을 기다리는 중 */
	PENDING,

	/** 부모가 승인했고 부모→자녀 송금까지 끝남 */
	APPROVED,

	/** 승인을 시도했지만 송금이 실패해, 차감했던 포인트를 환급하고 종료함 */
	FAILED
}
