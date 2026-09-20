package com.bana.dotori.savings.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsProductType;
import com.bana.dotori.savings.repository.SavingsProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 기동 시마다 {@link SavingsProductType} 6개 값을 {@code savings_products} 행과 동기화한다.
 *
 * <p>이 코드베이스에서 "고정 카탈로그를 코드에서 DB로 시딩"하는 첫 사례다. 다른 카탈로그
 * (investment_products 등)는 시더 없이 비어 있거나 외부에서 채워지는데, 예적금은 부모/관리자
 * CRUD가 아예 없는 대신 {@code savings_subscriptions.savings_product_id} FK가 실제 행을
 * 요구하므로 앱이 스스로 채워야 한다. 이름·설명이 바뀌어도 코드(enum)가 always source of
 * truth이므로 매번 덮어써 동기화한다 — 존재하면 갱신, 없으면 생성(upsert).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SavingsProductSeeder implements ApplicationRunner {

	private final SavingsProductRepository savingsProductRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		for (SavingsProductType type : SavingsProductType.values()) {
			SavingsProduct product = savingsProductRepository.findByProductType(type)
				.map(existing -> {
					existing.applyType(type);
					return existing;
				})
				.orElseGet(() -> SavingsProduct.builder()
					.productType(type)
					.name(displayName(type))
					.description(type.getEducationGoal())
					.build());
			savingsProductRepository.save(product);
		}
		log.info("예적금 상품 {}개 동기화 완료", SavingsProductType.values().length);
	}

	private String displayName(SavingsProductType type) {
		return switch (type) {
			case ORDINARY -> "보통예금";
			case SIMPLE_INTEREST -> "단리예금";
			case COMPOUND_INTEREST -> "복리예금";
			case SMALL_INSTALLMENT -> "소액적금";
			case MID_TERM_INSTALLMENT -> "중기적금";
			case LONG_TERM_INSTALLMENT -> "장기적금";
		};
	}
}
