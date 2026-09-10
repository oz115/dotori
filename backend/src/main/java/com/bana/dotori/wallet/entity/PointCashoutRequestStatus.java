package com.bana.dotori.wallet.entity;

/**
 * 환전 요청의 처리 상태.
 *
 * <p>{@code point_cashout_requests.status} 컬럼에 저장한다. 이 컬럼은 네이티브 ENUM
 * ({@code point_cashout_requests_status})이므로 여기 상수 이름이 그 타입의 라벨과
 * 정확히 같아야 한다.
 *
 * <p>{@link #PROGRESS}는 스키마에는 있지만 지금 구현은 쓰지 않는다(팀 결정). 승인은
 * {@link #PENDING}에서 바로 {@link #SUCCEED}/{@link #FAILED}로 끝난다.
 */
public enum PointCashoutRequestStatus {

	/** 부모 승인(또는 자녀 취소)을 기다리는 중 */
	PENDING,

	/** 지금 구현에서는 만들지 않는다. 비동기 처리로 갈 때를 위해 스키마에만 남겨 둔 값 */
	PROGRESS,

	/** 자녀가 자기 PENDING 요청을 직접 취소함. 차감했던 포인트를 환급함 */
	CANCELLED,

	/** 부모가 요청을 거절함. 차감했던 포인트를 환급함 */
	DECLINED,

	/** 부모가 승인했고 부모→자녀 송금까지 끝남 */
	SUCCEED,

	/** 승인을 시도했지만 송금이 실패해, 차감했던 포인트를 환급하고 종료함 */
	FAILED
}
