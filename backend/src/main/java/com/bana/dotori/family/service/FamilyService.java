package com.bana.dotori.family.service;

import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;

public interface FamilyService {

	FamilyCreateResponse create(Integer userId, FamilyCreateRequest request);
}
