package com.bana.dotori.store.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.store.dto.StoreProductCreateRequest;
import com.bana.dotori.store.dto.StoreProductDetailResponse;
import com.bana.dotori.store.dto.StoreProductListResponse;
import com.bana.dotori.store.dto.StoreProductUpdateRequest;
import com.bana.dotori.store.service.StoreProductService;

import jakarta.validation.Valid;
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

	@GetMapping("/{productId}")
	public ResponseEntity<StoreProductDetailResponse> getProduct(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer productId) {
		return ResponseEntity.ok(storeProductService.getProduct(userId, familyId, productId));
	}

	@PostMapping
	public ResponseEntity<StoreProductDetailResponse> createProduct(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @Valid @RequestBody StoreProductCreateRequest request) {
		StoreProductDetailResponse response = storeProductService.createProduct(userId, familyId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PatchMapping("/{productId}")
	public ResponseEntity<StoreProductDetailResponse> updateProduct(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer productId,
		@Valid @RequestBody StoreProductUpdateRequest request) {
		return ResponseEntity.ok(storeProductService.updateProduct(userId, familyId, productId, request));
	}

	@DeleteMapping("/{productId}")
	public ResponseEntity<Void> deleteProduct(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer productId) {
		storeProductService.deleteProduct(userId, familyId, productId);
		return ResponseEntity.noContent().build();
	}
}
