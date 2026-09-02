package com.bana.dotori.family.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.dto.FamilyMembersResponse;
import com.bana.dotori.family.dto.FamilyResponse;
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

	@GetMapping("/{familyId}")
	public ResponseEntity<FamilyResponse> getFamily(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId) {
		return ResponseEntity.ok(familyService.getFamily(userId, familyId));
	}

	@GetMapping("/{familyId}/members")
	public ResponseEntity<FamilyMembersResponse> getMembers(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId) {
		return ResponseEntity.ok(familyService.getMembers(userId, familyId));
	}

	@DeleteMapping("/{familyId}")
	public ResponseEntity<Void> disband(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId) {
		familyService.disband(userId, familyId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{familyId}/children/{childId}")
	public ResponseEntity<Void> removeChild(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId) {
		familyService.removeChild(userId, familyId, childId);
		return ResponseEntity.noContent().build();
	}
}
