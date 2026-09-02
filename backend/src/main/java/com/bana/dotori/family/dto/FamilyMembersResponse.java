package com.bana.dotori.family.dto;

import java.util.List;

public record FamilyMembersResponse(
	List<FamilyMemberResponse> members
) {
}
