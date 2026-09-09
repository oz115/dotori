package com.bana.dotori.family.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.family.dto.FamilyConnectResponse;
import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.dto.FamilyMemberResponse;
import com.bana.dotori.family.dto.FamilyMembersResponse;
import com.bana.dotori.family.dto.FamilyResponse;
import com.bana.dotori.family.dto.FamilyUpdateResponse;
import com.bana.dotori.family.dto.InvitationCodeResponse;
import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.entity.FamilyCode;
import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.global.util.PointExchangePolicy;
import com.bana.dotori.user.entity.User;

@Component
public class FamilyMapper {

	/** 금액 두 개는 이미 포인트로 환산된 값을 받는다. 원→포인트 환산은 서비스가 한다. */
	public Family toEntity(FamilyCreateRequest request, Integer createdBy, int allowancePoints,
		int rewardLimitPoints) {
		return Family.builder()
			.name(request.name())
			.createdBy(createdBy)
			.weeklyAllowance(allowancePoints)
			.weeklyRewardLimit(rewardLimitPoints)
			.financialKnowledgeLevel(request.financialKnowledgeLevel())
			.build();
	}

	public FamilyCreateResponse toCreateResponse(Family family) {
		return new FamilyCreateResponse(family.getId(), family.getName(), family.getCreatedBy(),
			family.getCreatedAt());
	}

	/** 저장된 포인트를 요청과 같은 단위(원)로 되돌려 응답한다. */
	public FamilyUpdateResponse toUpdateResponse(Family family) {
		return new FamilyUpdateResponse(family.getId(), family.getName(),
			family.getWeeklyAllowance() * PointExchangePolicy.KRW_PER_POINT,
			family.getWeeklyRewardLimit() * PointExchangePolicy.KRW_PER_POINT,
			family.getFinancialKnowledgeLevel(), family.getUpdatedAt());
	}

	public FamilyResponse toFamilyResponse(Family family, int memberCount) {
		return new FamilyResponse(family.getId(), family.getName(), family.getCreatedBy(), memberCount,
			family.getCreatedAt());
	}

	public FamilyMembersResponse toMembersResponse(List<FamilyMember> members) {
		return new FamilyMembersResponse(members.stream()
			.map(this::toMemberResponse)
			.toList());
	}

	public FamilyMemberResponse toMemberResponse(FamilyMember member) {
		User user = member.getUser();
		return new FamilyMemberResponse(user.getId(), user.getName(), user.getRole(), member.getJoinedAt());
	}

	public InvitationCodeResponse toInvitationCodeResponse(FamilyCode familyCode) {
		return new InvitationCodeResponse(familyCode.getFamily().getId(), familyCode.getCode(),
			familyCode.getExpiredAt());
	}

	public FamilyConnectResponse toConnectResponse(Family family, FamilyMember member) {
		return new FamilyConnectResponse(family.getId(), family.getName(), member.getJoinedAt());
	}
}
