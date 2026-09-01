package com.bana.dotori.store;

import com.bana.dotori.global.error.ErrorCode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointDeductionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SHOP-06 상점 물품 구매. 포인트 차감(WLT-05)의 첫 호출자다.
 *
 * <p>상점 도메인이 아직 없어 목업 저장소 위에서 돈다. 이 서비스를 지금 만든 이유는
 * 차감 서비스가 실제 사용 흐름에서 제대로 동작하는지 검증할 호출자가 필요해서다.
 * 상점 담당자의 구현이 들어오면 이 클래스는 참고용으로 두고 지우거나,
 * {@link StoreRepository} 구현만 바꿔 그대로 쓸 수 있다.
 *
 * <p>검증 순서가 중요하다. 재고와 하루 1회 제한을 먼저 확인하고 마지막에 포인트를 차감한다.
 * 포인트를 먼저 빼면 재고가 없어 되돌려야 하는 상황이 생기는데, 롤백으로 해결되긴 하지만
 * 굳이 잔액을 건드렸다 되돌릴 이유가 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorePurchaseService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final StoreRepository storeRepository;
	private final PointDeductionService pointDeductionService;

	@Transactional
	public StorePurchaseResult purchase(Integer familyId, Integer childId, Integer storeProductId, int quantity) {
		StoreProductSnapshot product = storeRepository.findProduct(familyId, storeProductId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
						"상품을 찾을 수 없습니다. familyId=" + familyId + ", storeProductId=" + storeProductId));

		if (storeRepository.hasPurchasedOn(childId, storeProductId, LocalDate.now(KST))) {
			throw new BusinessException(ErrorCode.DAILY_LIMIT_EXCEEDED,
					"오늘 이미 구매한 상품입니다. storeProductId=" + storeProductId);
		}

		if (!product.hasStock(quantity)) {
			throw new BusinessException(ErrorCode.OUT_OF_STOCK,
					"재고가 부족합니다. 남은 수량=" + product.quantity() + ", 요청 수량=" + quantity);
		}

		int totalPrice = product.totalPrice(quantity);
		Integer purchaseId = storeRepository.savePurchase(
				familyId, childId, storeProductId, quantity, product.price());

		// 멱등키에 구매 ID 를 넣어 두면, 같은 구매로 두 번 차감되는 일이 없다.
		PointChangeResult deduction = pointDeductionService.deduct(
				childId, totalPrice, PointTransactionReason.PURCHASE,
				product.name() + " 구매", "purchase-" + purchaseId);

		storeRepository.reduceStock(storeProductId, quantity);

		log.info("상점 구매 완료. childId={}, storeProductId={}, totalPrice={}, 잔액={}",
				childId, storeProductId, totalPrice, deduction.balanceAfter());

		return new StorePurchaseResult(purchaseId, storeProductId, product.name(), quantity,
				product.price(), totalPrice, deduction.balanceAfter(), OffsetDateTime.now(KST));
	}
}
