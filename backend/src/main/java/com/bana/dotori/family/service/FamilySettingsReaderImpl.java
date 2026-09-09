package com.bana.dotori.family.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.dto.FamilySettings;
import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilySettingsReaderImpl implements FamilySettingsReader {

	private final FamilyRepository familyRepository;

	@Override
	@Transactional(readOnly = true)
	public FamilySettings read(Integer familyId, Integer childId) {
		// childId 는 아직 쓰지 않는다 — 자녀별 오버라이드가 생기면 여기서 먼저 찾는다.
		Family family = familyRepository.findByIdAndDeletedAtIsNull(familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		return new FamilySettings(family.getWeeklyAllowance(), family.getWeeklyRewardLimit(),
			family.getFinancialKnowledgeLevel());
	}
}
