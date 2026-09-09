package com.bana.dotori.store.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.store.dto.StorePurchaseResponse;
import com.bana.dotori.store.entity.Purchase;
import com.bana.dotori.store.entity.StoreProduct;
import com.bana.dotori.store.repository.PurchaseRepository;
import com.bana.dotori.store.repository.StoreProductRepository;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointDeductionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SHOP-06 상점 물품 구매.
 *
 * <p>검증 순서가 중요하다. 가격 산정 여부·재고·하루 1회 제한을 먼저 확인하고 마지막에
 * 포인트를 차감한다. 포인트를 먼저 빼면 재고가 없어 되돌려야 하는 상황이 생기는데,
 * 롤백으로 해결되긴 하지만 굳이 잔액을 건드렸다 되돌릴 이유가 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorePurchaseService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final StoreProductRepository storeProductRepository;
	private final PurchaseRepository purchaseRepository;
	private final PointDeductionService pointDeductionService;

	@Transactional
	public StorePurchaseResponse purchase(Integer familyId, Integer childId, Integer storeProductId, int quantity) {
		// 재고 확인부터 차감까지 이 트랜잭션 동안 행을 잠근다 — 동시에 들어온 다른 구매가
		// 재고를 가로채 마지막 한 개가 두 명에게 팔리는 것을 막는다.
		StoreProduct product = storeProductRepository
			.findByIdAndFamilyIdAndDeletedAtIsNullForUpdate(storeProductId, familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

		if (product.getPrice() == null) {
			// AI 가 아직 가치(value)를 산정하지 못해 price 가 비어 있는 상태 — 구매 자체를 막는다.
			throw new BusinessException(ErrorCode.PRICE_NOT_EVALUATED);
		}

		LocalDate today = LocalDate.now(KST);
		if (purchaseRepository.existsByChildIdAndStoreProductIdAndPurchaseDate(childId, storeProductId, today)) {
			throw new BusinessException(ErrorCode.DAILY_LIMIT_EXCEEDED);
		}

		if (product.getQuantity() < quantity) {
			throw new BusinessException(ErrorCode.OUT_OF_STOCK);
		}

		int unitPrice = product.getPrice();
		int totalPrice = unitPrice * quantity;

		// 멱등키는 하루 1회 제한 덕분에 아이·상품·날짜 조합만으로 충분히 유일하다.
		String idempotencyKey = "purchase-" + childId + "-" + storeProductId + "-" + today;
		PointChangeResult deduction = pointDeductionService.deduct(
			childId, totalPrice, PointTransactionReason.PURCHASE, product.getName() + " 구매", idempotencyKey);

		Purchase purchase = purchaseRepository.save(Purchase.builder()
			.familyId(familyId)
			.childId(childId)
			.storeProductId(storeProductId)
			.quantity(quantity)
			.price(unitPrice)
			.purchaseDate(today)
			.build());
		product.reduceStock(quantity);

		log.info("상점 구매 완료. childId={}, storeProductId={}, totalPrice={}, 잔액={}",
			childId, storeProductId, totalPrice, deduction.balanceAfter());

		return new StorePurchaseResponse(purchase.getId(), storeProductId, product.getName(), quantity,
			unitPrice, totalPrice, deduction.balanceAfter(), OffsetDateTime.now(KST));
	}
}
