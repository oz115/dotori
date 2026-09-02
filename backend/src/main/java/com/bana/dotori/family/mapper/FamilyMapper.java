package com.bana.dotori.family.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.dto.FamilyMemberResponse;
import com.bana.dotori.family.dto.FamilyMembersResponse;
import com.bana.dotori.family.dto.FamilyResponse;
import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.user.entity.User;

@Component
public class FamilyMapper {

	public Family toEntity(FamilyCreateRequest request, Integer createdBy) {
		return Family.builder()
			.name(request.name())
			.createdBy(createdBy)
			.build();
	}

	public FamilyCreateResponse toCreateResponse(Family family) {
		return new FamilyCreateResponse(family.getId(), family.getName(), family.getCreatedBy(),
			family.getCreatedAt());
	}

	public FamilyResponse toFamilyResponse(Family family, long memberCount) {
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
}
