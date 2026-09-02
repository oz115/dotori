package com.bana.dotori.family.service;

import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.dto.FamilyMembersResponse;
import com.bana.dotori.family.dto.FamilyResponse;

public interface FamilyService {

	FamilyCreateResponse create(Integer userId, FamilyCreateRequest request);

	FamilyResponse getFamily(Integer userId, Integer familyId);

	FamilyMembersResponse getMembers(Integer userId, Integer familyId);
}
