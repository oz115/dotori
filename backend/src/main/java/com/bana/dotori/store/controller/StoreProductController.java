package com.bana.dotori.store.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.store.dto.StoreProductListResponse;
import com.bana.dotori.store.service.StoreProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/families/{familyId}/store/products")
@RequiredArgsConstructor
public class StoreProductController {

	private final StoreProductService storeProductService;

	@GetMapping
	public ResponseEntity<StoreProductListResponse> getProducts(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId) {
		return ResponseEntity.ok(storeProductService.getProducts(userId, familyId));
	}
}
