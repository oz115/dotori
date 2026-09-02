package com.bana.dotori.family.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.service.FamilyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
public class FamilyController {

	private final FamilyService familyService;

	@PostMapping
	public ResponseEntity<FamilyCreateResponse> create(@AuthenticationPrincipal Integer userId,
		@Valid @RequestBody FamilyCreateRequest request) {
		FamilyCreateResponse response = familyService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
