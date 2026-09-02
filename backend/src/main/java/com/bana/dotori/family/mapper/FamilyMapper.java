package com.bana.dotori.family.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.entity.Family;

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
}
