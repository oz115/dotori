package com.bana.dotori.chore.entity;

/**
 * 집안일 완료 요청의 상태 (DB ENUM {@code chore_completion_requests_status}).
 *
 * <p>PENDING 에서 시작해 SUCCEED · REJECTED · CANCELLED 중 하나로 한 번만 옮겨 간다.
 * 되돌아오는 전이는 없다. 부모가 승인을 물리려면 상태를 되돌리는 게 아니라
 * 새 요청을 받아야 한다 — 이미 지급한 포인트를 상태 변경만으로 회수할 수는 없기 때문이다.
 *
 * <p>PROGRESS 는 DB ENUM 에 있지만 쓰지 않는다. 승인이 외부 호출 없이 한 트랜잭션에서
 * 끝나서 "처리 중" 으로 머무를 구간이 없다. 값을 빼지 않고 두는 것은 다른 요청 상태
 * ENUM(현금화·예적금)과 모양을 맞춰 두기 위해서다.
 */
public enum ChoreCompletionStatus {

	PENDING,
	PROGRESS,
	CANCELLED,
	SUCCEED,
	REJECTED;

	/** 부모가 아직 처리하지 않아 승인·거절·취소를 받을 수 있는 상태인가. */
	public boolean isPending() {
		return this == PENDING;
	}
}
