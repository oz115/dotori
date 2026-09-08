package com.bana.dotori.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.store.dto.StoreProductCreateRequest;
import com.bana.dotori.store.dto.StoreProductDetailResponse;
import com.bana.dotori.store.dto.StoreProductListResponse;
import com.bana.dotori.store.dto.StoreProductUpdateRequest;
import com.bana.dotori.store.entity.StoreProduct;
import com.bana.dotori.store.entity.StoreProductCategory;
import com.bana.dotori.store.event.StoreProductCreatedEvent;
import com.bana.dotori.store.mapper.StoreProductMapper;
import com.bana.dotori.store.repository.StoreProductRepository;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

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

	@Mock
	private UserRepository userRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Spy
	private StoreProductMapper storeProductMapper;

	@InjectMocks
	private StoreProductServiceImpl storeProductService;

	private User user(UserRole role) {
		User user = User.builder()
			.loginId("login")
			.password("pw")
			.name("이름")
			.email("test@example.com")
			.role(role)
			.build();
		ReflectionTestUtils.setField(user, "id", USER_ID);
		return user;
	}

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

	private StoreProduct productWithId(Integer id, String name, StoreProductCategory category,
			BigDecimal value, Integer price, int quantity) {
		StoreProduct product = StoreProduct.builder()
			.familyId(FAMILY_ID)
			.createdBy(USER_ID)
			.name(name)
			.description("설명")
			.category(category)
			.value(value)
			.price(price)
			.quantity(quantity)
			.build();
		ReflectionTestUtils.setField(product, "id", id);
		return product;
	}

	@Test
	@DisplayName("가족 구성원이면 상품 목록을 반환한다")
	void getProducts_success() {
		givenActiveMember();
		given(storeProductRepository.findAllByFamilyIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(FAMILY_ID))
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
		given(storeProductRepository.findAllByFamilyIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(FAMILY_ID))
			.willReturn(List.of());

		StoreProductListResponse response = storeProductService.getProducts(USER_ID, FAMILY_ID);

		assertThat(response.products()).isEmpty();
	}

	@Test
	@DisplayName("가격 산정 전(value 미산정) 상품은 price가 null로 나간다")
	void getProducts_priceNotEvaluatedYet() {
		givenActiveMember();
		given(storeProductRepository.findAllByFamilyIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(FAMILY_ID))
			.willReturn(List.of(product("자전거", StoreProductCategory.TOY, null, 1)));

		StoreProductListResponse response = storeProductService.getProducts(USER_ID, FAMILY_ID);

		assertThat(response.products().get(0).price()).isNull();
	}

	@Test
	@DisplayName("상품 상세 조회 성공 시 전체 필드를 반환한다")
	void getProduct_success() {
		givenActiveMember();
		StoreProduct product = productWithId(5, "자전거", StoreProductCategory.TOY,
			new BigDecimal("90.00"), 900000, 1);
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(5, FAMILY_ID)).willReturn(Optional.of(product));

		StoreProductDetailResponse response = storeProductService.getProduct(USER_ID, FAMILY_ID, 5);

		assertThat(response.storeProductId()).isEqualTo(5);
		assertThat(response.name()).isEqualTo("자전거");
		assertThat(response.description()).isEqualTo("설명");
		assertThat(response.category()).isEqualTo(StoreProductCategory.TOY);
		assertThat(response.value()).isEqualByComparingTo("90.00");
		assertThat(response.price()).isEqualTo(900000);
		assertThat(response.quantity()).isEqualTo(1);
		assertThat(response.createdBy()).isEqualTo(USER_ID);
	}

	@Test
	@DisplayName("AI 가치 산정 전 상품은 value와 price가 null로 나간다")
	void getProduct_notEvaluatedYet() {
		givenActiveMember();
		StoreProduct product = productWithId(6, "자전거", StoreProductCategory.TOY, null, null, 1);
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(6, FAMILY_ID)).willReturn(Optional.of(product));

		StoreProductDetailResponse response = storeProductService.getProduct(USER_ID, FAMILY_ID, 6);

		assertThat(response.value()).isNull();
		assertThat(response.price()).isNull();
	}

	@Test
	@DisplayName("상품이 없으면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void getProduct_notFound() {
		givenActiveMember();
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(999, FAMILY_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> storeProductService.getProduct(USER_ID, FAMILY_ID, 999))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("다른 가족의 상품이면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void getProduct_otherFamilyProduct() {
		givenActiveMember();
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(7, FAMILY_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> storeProductService.getProduct(USER_ID, FAMILY_ID, 7))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("가족이 없으면 FAMILY_NOT_FOUND 예외가 발생한다")
	void getProducts_familyNotFound() {
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> storeProductService.getProducts(USER_ID, FAMILY_ID))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
		then(storeProductRepository).should(never()).findAllByFamilyIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(FAMILY_ID);
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
		then(storeProductRepository).should(never()).findAllByFamilyIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(FAMILY_ID);
	}

	private void givenSavedProductReturnsWithId(Integer id) {
		given(storeProductRepository.save(any())).willAnswer(invocation -> {
			StoreProduct saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", id);
			return saved;
		});
	}

	@Test
	@DisplayName("부모가 상품을 등록하면 value/price는 null인 상태로 생성되고 등록 이벤트가 발행된다")
	void createProduct_success() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		givenActiveMember();
		givenSavedProductReturnsWithId(99);
		StoreProductCreateRequest request = new StoreProductCreateRequest("자전거", "3개월 모아야 살 수 있음",
			StoreProductCategory.TOY, 1);

		StoreProductDetailResponse response = storeProductService.createProduct(USER_ID, FAMILY_ID, request);

		assertThat(response.storeProductId()).isEqualTo(99);
		assertThat(response.name()).isEqualTo("자전거");
		assertThat(response.category()).isEqualTo(StoreProductCategory.TOY);
		assertThat(response.value()).isNull();
		assertThat(response.price()).isNull();
		assertThat(response.quantity()).isEqualTo(1);

		var captor = org.mockito.ArgumentCaptor.forClass(StoreProductCreatedEvent.class);
		then(eventPublisher).should().publishEvent(captor.capture());
		StoreProductCreatedEvent event = captor.getValue();
		assertThat(event.storeProductId()).isEqualTo(99);
		assertThat(event.familyId()).isEqualTo(FAMILY_ID);
		assertThat(event.name()).isEqualTo("자전거");
		assertThat(event.category()).isEqualTo(StoreProductCategory.TOY);
	}

	@Test
	@DisplayName("자녀가 상품을 등록하려 하면 FORBIDDEN 예외가 발생한다")
	void createProduct_notParent() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.CHILD)));
		StoreProductCreateRequest request = new StoreProductCreateRequest("자전거", null, StoreProductCategory.TOY, 1);

		assertThatThrownBy(() -> storeProductService.createProduct(USER_ID, FAMILY_ID, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FORBIDDEN);
		then(storeProductRepository).should(never()).save(any());
		then(eventPublisher).should(never()).publishEvent(any());
	}

	@Test
	@DisplayName("존재하지 않는 사용자가 등록을 시도하면 UNAUTHORIZED 예외가 발생한다")
	void createProduct_userNotFound() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.empty());
		StoreProductCreateRequest request = new StoreProductCreateRequest("자전거", null, StoreProductCategory.TOY, 1);

		assertThatThrownBy(() -> storeProductService.createProduct(USER_ID, FAMILY_ID, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
		then(storeProductRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("존재하지 않는 가족에 등록하려 하면 FAMILY_NOT_FOUND 예외가 발생한다")
	void createProduct_familyNotFound() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID)).willReturn(Optional.empty());
		StoreProductCreateRequest request = new StoreProductCreateRequest("자전거", null, StoreProductCategory.TOY, 1);

		assertThatThrownBy(() -> storeProductService.createProduct(USER_ID, FAMILY_ID, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
		then(storeProductRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("소속되지 않은 가족에 등록하려 하면 FAMILY_NOT_FOUND 예외가 발생한다")
	void createProduct_notFamilyMember() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID))
			.willReturn(Optional.of(Family.builder().name("남의 가족").createdBy(99).build()));
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, USER_ID)).willReturn(false);
		StoreProductCreateRequest request = new StoreProductCreateRequest("자전거", null, StoreProductCategory.TOY, 1);

		assertThatThrownBy(() -> storeProductService.createProduct(USER_ID, FAMILY_ID, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
		then(storeProductRepository).should(never()).save(any());
		then(eventPublisher).should(never()).publishEvent(any());
	}

	@Test
	@DisplayName("일부 필드만 보내면 그 필드만 바뀌고 나머지는 유지된다")
	void updateProduct_partial() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		givenActiveMember();
		StoreProduct product = productWithId(5, "자전거", StoreProductCategory.TOY, new BigDecimal("90.00"), 900000, 1);
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(5, FAMILY_ID)).willReturn(Optional.of(product));
		StoreProductUpdateRequest request = new StoreProductUpdateRequest("자전거(중고)", null, null, null, null);

		StoreProductDetailResponse response = storeProductService.updateProduct(USER_ID, FAMILY_ID, 5, request);

		assertThat(response.name()).isEqualTo("자전거(중고)");
		assertThat(response.description()).isEqualTo("설명");
		assertThat(response.category()).isEqualTo(StoreProductCategory.TOY);
		assertThat(response.price()).isEqualTo(900000);
		assertThat(response.quantity()).isEqualTo(1);
	}

	@Test
	@DisplayName("name/description/category가 바뀌어도 기존 value/price는 그대로 유지된다")
	void updateProduct_doesNotResetValueOrPrice() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		givenActiveMember();
		StoreProduct product = productWithId(5, "자전거", StoreProductCategory.TOY, new BigDecimal("90.00"), 900000, 1);
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(5, FAMILY_ID)).willReturn(Optional.of(product));
		StoreProductUpdateRequest request = new StoreProductUpdateRequest("전동 자전거", "설명 변경",
			StoreProductCategory.OUTING, null, null);

		StoreProductDetailResponse response = storeProductService.updateProduct(USER_ID, FAMILY_ID, 5, request);

		assertThat(response.name()).isEqualTo("전동 자전거");
		assertThat(response.category()).isEqualTo(StoreProductCategory.OUTING);
		assertThat(response.value()).isEqualByComparingTo("90.00");
		assertThat(response.price()).isEqualTo(900000);
	}

	@Test
	@DisplayName("부모가 price를 직접 수정하면 반영된다")
	void updateProduct_priceOverride() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		givenActiveMember();
		StoreProduct product = productWithId(5, "자전거", StoreProductCategory.TOY, new BigDecimal("90.00"), 900000, 1);
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(5, FAMILY_ID)).willReturn(Optional.of(product));
		StoreProductUpdateRequest request = new StoreProductUpdateRequest(null, null, null, 5000, null);

		StoreProductDetailResponse response = storeProductService.updateProduct(USER_ID, FAMILY_ID, 5, request);

		assertThat(response.price()).isEqualTo(5000);
	}

	@Test
	@DisplayName("이름을 빈 문자열로 보내면 INVALID_REQUEST 예외가 발생한다")
	void updateProduct_blankName() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		givenActiveMember();
		StoreProductUpdateRequest request = new StoreProductUpdateRequest("   ", null, null, null, null);

		assertThatThrownBy(() -> storeProductService.updateProduct(USER_ID, FAMILY_ID, 5, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_REQUEST);
		then(storeProductRepository).should(never()).findByIdAndFamilyIdAndDeletedAtIsNull(any(), any());
	}

	@Test
	@DisplayName("자녀가 수정을 시도하면 FORBIDDEN 예외가 발생한다")
	void updateProduct_notParent() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.CHILD)));
		StoreProductUpdateRequest request = new StoreProductUpdateRequest("이름", null, null, null, null);

		assertThatThrownBy(() -> storeProductService.updateProduct(USER_ID, FAMILY_ID, 5, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("존재하지 않는 사용자가 수정을 시도하면 UNAUTHORIZED 예외가 발생한다")
	void updateProduct_userNotFound() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.empty());
		StoreProductUpdateRequest request = new StoreProductUpdateRequest("이름", null, null, null, null);

		assertThatThrownBy(() -> storeProductService.updateProduct(USER_ID, FAMILY_ID, 5, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("존재하지 않는 가족에서 수정하려 하면 FAMILY_NOT_FOUND 예외가 발생한다")
	void updateProduct_familyNotFound() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID)).willReturn(Optional.empty());
		StoreProductUpdateRequest request = new StoreProductUpdateRequest("이름", null, null, null, null);

		assertThatThrownBy(() -> storeProductService.updateProduct(USER_ID, FAMILY_ID, 5, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("소속되지 않은 가족에서 수정하려 하면 FAMILY_NOT_FOUND 예외가 발생한다")
	void updateProduct_notFamilyMember() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID))
			.willReturn(Optional.of(Family.builder().name("남의 가족").createdBy(99).build()));
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, USER_ID)).willReturn(false);
		StoreProductUpdateRequest request = new StoreProductUpdateRequest("이름", null, null, null, null);

		assertThatThrownBy(() -> storeProductService.updateProduct(USER_ID, FAMILY_ID, 5, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("상품이 없으면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void updateProduct_productNotFound() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		givenActiveMember();
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(999, FAMILY_ID)).willReturn(Optional.empty());
		StoreProductUpdateRequest request = new StoreProductUpdateRequest("이름", null, null, null, null);

		assertThatThrownBy(() -> storeProductService.updateProduct(USER_ID, FAMILY_ID, 999, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("부모가 상품을 삭제하면 deletedAt이 채워진다(soft delete)")
	void deleteProduct_success() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		givenActiveMember();
		StoreProduct product = productWithId(5, "자전거", StoreProductCategory.TOY, null, null, 1);
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(5, FAMILY_ID))
			.willReturn(Optional.of(product));

		storeProductService.deleteProduct(USER_ID, FAMILY_ID, 5);

		assertThat(product.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("자녀가 삭제를 시도하면 FORBIDDEN 예외가 발생한다")
	void deleteProduct_notParent() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.CHILD)));

		assertThatThrownBy(() -> storeProductService.deleteProduct(USER_ID, FAMILY_ID, 5))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("존재하지 않는 사용자가 삭제를 시도하면 UNAUTHORIZED 예외가 발생한다")
	void deleteProduct_userNotFound() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> storeProductService.deleteProduct(USER_ID, FAMILY_ID, 5))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("존재하지 않는 가족에서 삭제하려 하면 FAMILY_NOT_FOUND 예외가 발생한다")
	void deleteProduct_familyNotFound() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> storeProductService.deleteProduct(USER_ID, FAMILY_ID, 5))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("소속되지 않은 가족에서 삭제하려 하면 FAMILY_NOT_FOUND 예외가 발생한다")
	void deleteProduct_notFamilyMember() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		given(familyRepository.findByIdAndDeletedAtIsNull(FAMILY_ID))
			.willReturn(Optional.of(Family.builder().name("남의 가족").createdBy(99).build()));
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, USER_ID)).willReturn(false);

		assertThatThrownBy(() -> storeProductService.deleteProduct(USER_ID, FAMILY_ID, 5))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("상품이 없거나 이미 삭제됐으면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void deleteProduct_notFound() {
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(UserRole.PARENT)));
		givenActiveMember();
		given(storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(999, FAMILY_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> storeProductService.deleteProduct(USER_ID, FAMILY_ID, 999))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
	}
}
