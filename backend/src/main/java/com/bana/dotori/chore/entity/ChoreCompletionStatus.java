package com.bana.dotori.chore.entity;

/**
 * 집안일 완료 요청의 상태 (DB ENUM {@code chore_completion_requests_status}).
 *
 * <p>PENDING 에서 시작해 SUCCEED · REJECTED · CANCELLED 중 하나로 한 번만 옮겨 간다.
 * 되돌아오는 전이는 없다. 부모가 승인을 물리려면 상태를 되돌리는 게 아니라
 * 새 요청을 받아야 한다 — 이미 지급한 포인트를 상태 변경만으로 회수할 수는 없기 때문이다.
 *
 * <p>"처리 중"에 해당하는 값을 두지 않는다. 아이가 사진과 내용을 한 번에 올려 요청이
 * 완성되고, 올린 뒤에는 그 요청을 고칠 수 없다. 부모가 거절하면 같은 행을 손보는 게 아니라
 * 아이가 사진과 내용을 다시 올려 <b>새 요청</b>을 만든다. 그래서 "올리는 중"이나
 * "고치는 중"으로 머무를 구간이 없다.
 *
 * <p>현금화(point_cashout_requests)처럼 승인 뒤 외부 송금을 기다리는 요청에는 그 구간이
 * 있어 PROGRESS 를 둔다. 집안일 승인은 지급까지 한 트랜잭션에서 끝나므로 필요 없다.
 */
public enum ChoreCompletionStatus {

	PENDING,
	CANCELLED,
	SUCCEED,
	REJECTED;

	/** 부모가 아직 처리하지 않아 승인·거절·취소를 받을 수 있는 상태인가. */
	public boolean isPending() {
		return this == PENDING;
	}
}
