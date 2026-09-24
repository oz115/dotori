package com.bana.dotori.store.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.store.dto.PurchaseHistoryResponse;
import com.bana.dotori.store.service.PurchaseHistoryService;

import lombok.RequiredArgsConstructor;

/**
 * SHOP-07 구매 내역 조회. 지갑 거래내역(WLT-06)과 같은 URL 형태·권한 규칙을 따른다.
 */
@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/store/purchases")
@RequiredArgsConstructor
public class PurchaseController {

	private final PurchaseHistoryService purchaseHistoryService;

	@GetMapping
	public PurchaseHistoryResponse getPurchases(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId,
		@RequestParam(required = false) LocalDate startDate,
		@RequestParam(required = false) LocalDate endDate) {
		return purchaseHistoryService.getPurchases(familyId, childId, userId, startDate, endDate);
	}
}
