package com.bana.dotori.mock.store;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

/**
 * 상점 도메인이 완성될 때까지 쓰는 메모리 구현.
 *
 * <p>포인트 차감(WLT-05)의 호출자 역할을 대신해, 차감이 실제 흐름에서 제대로 동작하는지
 * 검증할 수 있게 해 준다. 상점 담당자의 작업이 들어오면 이 클래스를 지우고
 * {@link StoreRepository} 를 구현한 진짜 저장소로 바꾸면 된다.
 *
 * <p>메모리에만 있으므로 재고와 구매 이력은 애플리케이션을 재시작하면 사라진다.
 * 또 포인트 차감은 DB 트랜잭션인데 이쪽은 메모리라, 트랜잭션이 롤백돼도 재고는 되돌아오지 않는다.
 * 진짜 상점 저장소가 들어오면 자연히 해결되는 문제지만, 목업을 쓰는 동안에는 알고 있어야 한다.
 */
@Slf4j
@Repository
public class MockStoreRepository implements StoreRepository {

	private final Map<Integer, StoreProductSnapshot> productsById = new ConcurrentHashMap<>();
	private final Set<String> purchasedDays = ConcurrentHashMap.newKeySet();
	private final Map<Integer, PurchaseRecord> purchasesById = new ConcurrentHashMap<>();
	private final AtomicInteger productIdSequence = new AtomicInteger(1);
	private final AtomicInteger purchaseIdSequence = new AtomicInteger(1);

	@Override
	public Optional<StoreProductSnapshot> findProduct(Integer familyId, Integer storeProductId) {
		return Optional.ofNullable(productsById.get(storeProductId))
				.filter(product -> product.familyId().equals(familyId));
	}

	@Override
	public synchronized void reduceStock(Integer storeProductId, int quantity) {
		StoreProductSnapshot product = productsById.get(storeProductId);
		if (product != null) {
			productsById.put(storeProductId, product.reduceStock(quantity));
		}
	}

	@Override
	public boolean hasPurchasedOn(Integer childId, Integer storeProductId, LocalDate date) {
		return purchasedDays.contains(dailyKey(childId, storeProductId, date));
	}

	@Override
	public Integer savePurchase(Integer familyId, Integer childId, Integer storeProductId,
			int quantity, int unitPrice, LocalDate purchaseDate) {
		Integer purchaseId = purchaseIdSequence.getAndIncrement();
		purchasesById.put(purchaseId,
				new PurchaseRecord(purchaseId, familyId, childId, storeProductId, quantity, unitPrice));
		purchasedDays.add(dailyKey(childId, storeProductId, purchaseDate));
		log.warn("목업 상점에 구매 이력을 남겼다. 실제 DB 에는 저장되지 않는다. purchaseId={}", purchaseId);
		return purchaseId;
	}

	/**
	 * 테스트와 데모에서 상품을 등록할 때 쓴다.
	 */
	public StoreProductSnapshot registerProduct(Integer familyId, String name, int price, int quantity) {
		StoreProductSnapshot product = new StoreProductSnapshot(
				productIdSequence.getAndIncrement(), familyId, name, price, quantity);
		productsById.put(product.storeProductId(), product);
		return product;
	}

	public void clear() {
		productsById.clear();
		purchasedDays.clear();
		purchasesById.clear();
	}

	private String dailyKey(Integer childId, Integer storeProductId, LocalDate date) {
		return childId + ":" + storeProductId + ":" + date;
	}

	private record PurchaseRecord(Integer purchaseId, Integer familyId, Integer childId,
			Integer storeProductId, int quantity, int unitPrice) {
	}
}
