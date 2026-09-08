package com.bana.dotori.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.family.dto.FamilySettings;
import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.entity.FinancialKnowledgeLevel;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class FamilySettingsReaderImplTest {

	@Mock
	private FamilyRepository familyRepository;

	@InjectMocks
	private FamilySettingsReaderImpl familySettingsReader;

	@Test
	@DisplayName("가족의 온보딩 설정값(포인트 단위)을 반환한다")
	void read_success() {
		Family family = Family.builder()
			.name("도토리네 가족")
			.createdBy(1)
			.weeklyAllowance(1000)
			.weeklyRewardLimit(500)
			.financialKnowledgeLevel(FinancialKnowledgeLevel.INTERMEDIATE)
			.build();
		given(familyRepository.findByIdAndDeletedAtIsNull(1)).willReturn(Optional.of(family));

		FamilySettings settings = familySettingsReader.read(1, 7);

		assertThat(settings.weeklyAllowance()).isEqualTo(1000);
		assertThat(settings.weeklyRewardLimit()).isEqualTo(500);
		assertThat(settings.financialKnowledgeLevel()).isEqualTo(FinancialKnowledgeLevel.INTERMEDIATE);
	}

	@Test
	@DisplayName("가족이 없거나 해체됐으면 FAMILY_NOT_FOUND 예외가 발생한다")
	void read_familyNotFound() {
		given(familyRepository.findByIdAndDeletedAtIsNull(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> familySettingsReader.read(99, 7))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}
}
