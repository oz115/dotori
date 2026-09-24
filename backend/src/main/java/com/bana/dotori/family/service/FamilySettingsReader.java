package com.bana.dotori.family.service;

import com.bana.dotori.family.dto.FamilySettings;

/**
 * 온보딩 설정값(기본 용돈·추가 보상 한도·금융 지식 수준)을 읽는 유일한 통로.
 *
 * <p>소비처(퀴즈·예적금·투자·집안일)는 families 컬럼을 직접 조회하지 말고 이 인터페이스를
 * 주입받는다. 지금은 가족 기본값 하나지만, 자녀별 차등 설정이 생기면 이 구현체가
 * 자녀 오버라이드를 먼저 찾도록 바뀔 뿐 소비처 코드는 그대로다 — childId 를 지금부터
 * 받는 이유다.
 */
public interface FamilySettingsReader {

	/**
	 * 자녀에게 적용할 가족 설정값을 읽는다.
	 *
	 * @param familyId 가족 ID
	 * @param childId  설정을 적용할 자녀 ID (현재는 가족 기본값이라 조회에 쓰지 않는다)
	 * @throws com.bana.dotori.global.error.BusinessException FAMILY_NOT_FOUND 가족이 없거나 해체된 경우
	 */
	FamilySettings read(Integer familyId, Integer childId);
}
