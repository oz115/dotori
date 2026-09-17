package com.bana.dotori.family.event;

/**
 * 아이가 초대 코드로 가족에 합류했다는 애플리케이션 이벤트 (FAM-05).
 *
 * <p>서비스 트랜잭션 안에서 발행만 하고 알림은 리스너가 커밋 이후에 보낸다. 합류가
 * 롤백되면 리스너가 호출되지 않으므로, 실제로는 들어오지 않은 아이를 부모에게 알리는
 * 일이 없다.
 */
public record FamilyChildConnectedEvent(
	Integer familyId,
	Integer childId,
	String childName,
	String familyName
) {
}
