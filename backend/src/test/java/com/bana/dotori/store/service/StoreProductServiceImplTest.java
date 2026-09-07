package com.bana.dotori.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.store.dto.StoreProductListResponse;
import com.bana.dotori.store.entity.StoreProduct;
import com.bana.dotori.store.entity.StoreProductCategory;
import com.bana.dotori.store.mapper.StoreProductMapper;
import com.bana.dotori.store.repository.StoreProductRepository;

@ExtendWith(MockitoExtension.class)
class StoreProductServiceImplTest {

	private static final Integer USER_ID = 1;
	private static final Integer FAMILY_ID = 10;

	@Mock
	private StoreProductRepository storeProductRepository;

	@Mock
	private FamilyRepository familyRepository;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Spy
	private StoreProductMapper storeProductMapper;

	@InjectMocks
	private StoreProductServiceImpl storeProductService;

	private StoreProduct product(String name, StoreProductCategory category, Integer price, int quantity) {
		return StoreProduct.builder()
			.familyId(FAMILY_ID)
			.createdBy(USER_ID)
			.name(name)
			.category(category)
			.price(price)
			.quantity(quantity)
			.build();
	}

	private void givenActiveMember() {
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID))
			.willReturn(Optional.of(Family.builder().name("도토리네").createdBy(USER_ID).build()));
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, USER_ID)).willReturn(true);
	}

	@Test
	@DisplayName("가족 구성원이면 상품 목록을 반환한다")
	void getProducts_success() {
		givenActiveMember();
		given(storeProductRepository.findAllByFamilyIdOrderByCreatedAtDescIdDesc(FAMILY_ID))
			.willReturn(List.of(
				product("게임 30분 이용권", StoreProductCategory.GAME, 3000, 5),
				product("아이스크림", StoreProductCategory.SNACK, 1500, 1)));

		StoreProductListResponse response = storeProductService.getProducts(USER_ID, FAMILY_ID);

		assertThat(response.products()).hasSize(2);
		assertThat(response.products().get(0).name()).isEqualTo("게임 30분 이용권");
		assertThat(response.products().get(0).category()).isEqualTo(StoreProductCategory.GAME);
		assertThat(response.products().get(0).price()).isEqualTo(3000);
		assertThat(response.products().get(1).quantity()).isEqualTo(1);
	}

	@Test
	@DisplayName("상품이 없으면 빈 목록을 반환한다")
	void getProducts_empty() {
		givenActiveMember();
		given(storeProductRepository.findAllByFamilyIdOrderByCreatedAtDescIdDesc(FAMILY_ID))
			.willReturn(List.of());

		StoreProductListResponse response = storeProductService.getProducts(USER_ID, FAMILY_ID);

		assertThat(response.products()).isEmpty();
	}

	@Test
	@DisplayName("가격 산정 전(value 미산정) 상품은 price가 null로 나간다")
	void getProducts_priceNotEvaluatedYet() {
		givenActiveMember();
		given(storeProductRepository.findAllByFamilyIdOrderByCreatedAtDescIdDesc(FAMILY_ID))
			.willReturn(List.of(product("자전거", StoreProductCategory.TOY, null, 1)));

		StoreProductListResponse response = storeProductService.getProducts(USER_ID, FAMILY_ID);

		assertThat(response.products().get(0).price()).isNull();
	}

	@Test
	@DisplayName("가족이 없으면 FAMILY_NOT_FOUND 예외가 발생한다")
	void getProducts_familyNotFound() {
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> storeProductService.getProducts(USER_ID, FAMILY_ID))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
		then(storeProductRepository).should(never()).findAllByFamilyIdOrderByCreatedAtDescIdDesc(FAMILY_ID);
	}

	@Test
	@DisplayName("가족 구성원이 아니면 FAMILY_NOT_FOUND 예외가 발생한다")
	void getProducts_notFamilyMember() {
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID))
			.willReturn(Optional.of(Family.builder().name("남의 가족").createdBy(99).build()));
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, USER_ID)).willReturn(false);

		assertThatThrownBy(() -> storeProductService.getProducts(USER_ID, FAMILY_ID))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
		then(storeProductRepository).should(never()).findAllByFamilyIdOrderByCreatedAtDescIdDesc(FAMILY_ID);
	}
}
