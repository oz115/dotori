package com.bana.dotori.wallet.repository;

/**
 * 지갑 기능이 필요로 하는 최소한의 가족 소속 조회.
 *
 * <p>가족 도메인(FAM-01~05)은 다른 담당자의 작업 범위다. 여기서 Family/FamilyMember
 * 엔티티를 만들면 그 브랜치와 충돌하므로, 필요한 질문 하나만 인터페이스로 뽑았다.
 * 가족 도메인이 완성되면 그쪽 서비스를 이 인터페이스에 연결하고 어댑터만 지우면 된다.
 */
public interface FamilyMembershipReader {

	/**
	 * 해당 사용자가 이 가족에 속한 자녀인지 확인한다.
	 *
	 * <p>탈퇴한(deleted_at 이 채워진) 사용자는 자녀로 보지 않는다.
	 */
	boolean isChildOfFamily(Integer familyId, Integer childId);
}
