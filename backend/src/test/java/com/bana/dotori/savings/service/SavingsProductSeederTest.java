package com.bana.dotori.savings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsProductType;
import com.bana.dotori.savings.repository.SavingsProductRepository;

/**
 * 2계층 - 6개 상품이 없으면 만들고 있으면 코드 값으로 갱신하는지만 검증한다 (upsert).
 */
@DisplayName("SavingsProductSeeder 동기화")
@ExtendWith(MockitoExtension.class)
class SavingsProductSeederTest {

	@Mock
	private SavingsProductRepository savingsProductRepository;

	@Mock
	private ApplicationArguments applicationArguments;

	@InjectMocks
	private SavingsProductSeeder savingsProductSeeder;

	@Test
	@DisplayName("행이 없으면 6개 전부 새로 만든다")
	void 없으면_6개를_새로_만든다() {
		given(savingsProductRepository.findByProductType(any())).willReturn(Optional.empty());

		savingsProductSeeder.run(applicationArguments);

		ArgumentCaptor<SavingsProduct> captor = ArgumentCaptor.forClass(SavingsProduct.class);
		verify(savingsProductRepository, times(SavingsProductType.values().length)).save(captor.capture());
		assertThat(captor.getAllValues())
			.extracting(SavingsProduct::getProductType)
			.containsExactlyInAnyOrder(SavingsProductType.values());
	}

	@Test
	@DisplayName("이미 있으면 새로 만들지 않고 코드 값으로 갱신만 한다")
	void 있으면_갱신만_한다() {
		SavingsProduct existing = SavingsProduct.builder()
			.productType(SavingsProductType.ORDINARY)
			.name("옛날 이름")
			.description("옛날 설명")
			.build();
		ReflectionTestUtils.setField(existing, "id", 1);

		given(savingsProductRepository.findByProductType(SavingsProductType.ORDINARY))
			.willReturn(Optional.of(existing));
		given(savingsProductRepository.findByProductType(org.mockito.ArgumentMatchers.argThat(
			type -> type != SavingsProductType.ORDINARY))).willReturn(Optional.empty());

		savingsProductSeeder.run(applicationArguments);

		verify(savingsProductRepository).save(existing);
		assertThat(existing.getInterestRate()).isEqualTo(SavingsProductType.ORDINARY.getInterestRate());
	}
}
