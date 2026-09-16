package com.bana.dotori.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.store.entity.Purchase;
import com.bana.dotori.store.entity.StoreProduct;
import com.bana.dotori.store.entity.StoreProductCategory;
import com.bana.dotori.store.event.StoreProductPurchasedEvent;
import com.bana.dotori.store.repository.PurchaseRepository;
import com.bana.dotori.store.repository.StoreProductRepository;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointDeductionService;

@ExtendWith(MockitoExtension.class)
class StorePurchaseServiceTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final Integer PARENT_ID = 1;
	private static final Integer PRODUCT_ID = 5;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Mock
	private StoreProductRepository storeProductRepository;

	@Mock
	private PurchaseRepository purchaseRepository;

	@Mock
	private PointDeductionService pointDeductionService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private StorePurchaseService storePurchaseService;

	private StoreProduct product(Integer price, int quantity) {
		StoreProduct product = StoreProduct.builder()
			.familyId(FAMILY_ID)
			.createdBy(PARENT_ID)
			.name("자전거")
			.category(StoreProductCategory.TOY)
			.price(price)
			.quantity(quantity)
			.build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
		return product;
	}

	@Test
	@DisplayName("구매에 성공하면 상품을 등록한 부모를 대상으로 구매 알림 이벤트가 발행된다")
	void purchase_publishesPurchasedEventForRegisteringParent() {
		StoreProduct product = product(900000, 3);
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNullForUpdate(PRODUCT_ID, FAMILY_ID))
			.willReturn(Optional.of(product));
		given(purchaseRepository.existsByChildIdAndStoreProductIdAndPurchaseDate(
			CHILD_ID, PRODUCT_ID, LocalDate.now(KST))).willReturn(false);
		given(pointDeductionService.deduct(any(), anyInt(), any(PointTransactionReason.class), any(),
			any())).willReturn(PointChangeResult.applied(
				PointTransaction.record(CHILD_ID, PointTransactionReason.PURCHASE, -900000, 100, "자전거 구매",
					"purchase-key")));
		given(purchaseRepository.save(any())).willAnswer(invocation -> {
			Purchase saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 77);
			return saved;
		});

		storePurchaseService.purchase(FAMILY_ID, CHILD_ID, PRODUCT_ID, 1);

		ArgumentCaptor<StoreProductPurchasedEvent> captor = ArgumentCaptor.forClass(StoreProductPurchasedEvent.class);
		then(eventPublisher).should().publishEvent(captor.capture());
		StoreProductPurchasedEvent event = captor.getValue();
		assertThat(event.purchaseId()).isEqualTo(77);
		assertThat(event.storeProductId()).isEqualTo(PRODUCT_ID);
		assertThat(event.parentUserId()).isEqualTo(PARENT_ID);
		assertThat(event.productName()).isEqualTo("자전거");
		assertThat(event.quantity()).isEqualTo(1);
		assertThat(event.totalPrice()).isEqualTo(900000);
	}
}
