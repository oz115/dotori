package com.bana.dotori.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.store.dto.PurchaseHistoryResponse;
import com.bana.dotori.store.entity.Purchase;
import com.bana.dotori.store.entity.StoreProduct;
import com.bana.dotori.store.entity.StoreProductCategory;
import com.bana.dotori.store.repository.PurchaseRepository;
import com.bana.dotori.store.repository.StoreProductRepository;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PurchaseHistoryServiceTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 1;
	private static final Integer PARENT_ID = 2;

	@Mock
	private PurchaseRepository purchaseRepository;

	@Mock
	private StoreProductRepository storeProductRepository;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private PurchaseHistoryService purchaseHistoryService;

	private User user(Integer id, UserRole role) {
		User user = User.builder()
			.loginId("login" + id)
			.password("pw")
			.name("이름")
			.email("test" + id + "@example.com")
			.role(role)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Purchase purchase(Integer id, String name, int quantity, int price) {
		Purchase purchase = Purchase.builder()
			.familyId(FAMILY_ID)
			.childId(CHILD_ID)
			.storeProductId(99)
			.productName(name)
			.quantity(quantity)
			.price(price)
			.purchaseDate(LocalDate.of(2026, 8, 28))
			.build();
		ReflectionTestUtils.setField(purchase, "id", id);
		return purchase;
	}

	private StoreProduct storeProduct(Integer id, StoreProductCategory category) {
		StoreProduct product = StoreProduct.builder()
			.familyId(FAMILY_ID)
			.createdBy(PARENT_ID)
			.name("상품")
			.category(category)
			.quantity(1)
			.build();
		ReflectionTestUtils.setField(product, "id", id);
		return product;
	}

	@Test
	@DisplayName("자녀가 본인 구매 내역을 조회하면 목록을 반환한다")
	void getPurchases_selfSuccess() {
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(userRepository.findById(CHILD_ID)).willReturn(Optional.of(user(CHILD_ID, UserRole.CHILD)));
		given(purchaseRepository.findAllByChildIdAndPeriod(CHILD_ID, null, null))
			.willReturn(List.of(purchase(1, "게임 30분 이용권", 1, 3000)));

		PurchaseHistoryResponse response = purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, CHILD_ID, null,
			null);

		assertThat(response.purchases()).hasSize(1);
		assertThat(response.purchases().get(0).purchaseId()).isEqualTo(1);
		assertThat(response.purchases().get(0).productName()).isEqualTo("게임 30분 이용권");
		assertThat(response.purchases().get(0).totalPrice()).isEqualTo(3000);
	}

	@Test
	@DisplayName("총 구매 금액은 단가 * 수량이다")
	void getPurchases_totalPriceIsUnitPriceTimesQuantity() {
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(userRepository.findById(CHILD_ID)).willReturn(Optional.of(user(CHILD_ID, UserRole.CHILD)));
		given(purchaseRepository.findAllByChildIdAndPeriod(CHILD_ID, null, null))
			.willReturn(List.of(purchase(1, "간식 쿠폰", 3, 2000)));

		PurchaseHistoryResponse response = purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, CHILD_ID, null,
			null);

		assertThat(response.purchases().get(0).totalPrice()).isEqualTo(6000);
	}

	@Test
	@DisplayName("구매한 상품이 살아있으면 지금 카테고리를 보여준다")
	void getPurchases_categoryFromLiveProduct() {
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(userRepository.findById(CHILD_ID)).willReturn(Optional.of(user(CHILD_ID, UserRole.CHILD)));
		given(purchaseRepository.findAllByChildIdAndPeriod(CHILD_ID, null, null))
			.willReturn(List.of(purchase(1, "게임 30분 이용권", 1, 3000)));
		given(storeProductRepository.findAllById(List.of(99)))
			.willReturn(List.of(storeProduct(99, StoreProductCategory.GAME)));

		PurchaseHistoryResponse response = purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, CHILD_ID, null,
			null);

		assertThat(response.purchases().get(0).category()).isEqualTo(StoreProductCategory.GAME);
	}

	@Test
	@DisplayName("구매한 상품이 삭제돼 조회되지 않으면 카테고리는 null이다")
	void getPurchases_categoryNullWhenProductGone() {
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(userRepository.findById(CHILD_ID)).willReturn(Optional.of(user(CHILD_ID, UserRole.CHILD)));
		given(purchaseRepository.findAllByChildIdAndPeriod(CHILD_ID, null, null))
			.willReturn(List.of(purchase(1, "게임 30분 이용권", 1, 3000)));
		given(storeProductRepository.findAllById(List.of(99))).willReturn(List.of());

		PurchaseHistoryResponse response = purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, CHILD_ID, null,
			null);

		assertThat(response.purchases().get(0).category()).isNull();
	}

	@Test
	@DisplayName("구매 내역이 없으면 빈 목록을 반환한다")
	void getPurchases_empty() {
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(userRepository.findById(CHILD_ID)).willReturn(Optional.of(user(CHILD_ID, UserRole.CHILD)));
		given(purchaseRepository.findAllByChildIdAndPeriod(CHILD_ID, null, null)).willReturn(List.of());

		PurchaseHistoryResponse response = purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, CHILD_ID, null,
			null);

		assertThat(response.purchases()).isEmpty();
	}

	@Test
	@DisplayName("부모가 같은 가족 자녀의 구매 내역을 조회하면 목록을 반환한다")
	void getPurchases_parentViewsChild() {
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(userRepository.findById(PARENT_ID)).willReturn(Optional.of(user(PARENT_ID, UserRole.PARENT)));
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, CHILD_ID)).willReturn(true);
		given(userRepository.findById(CHILD_ID)).willReturn(Optional.of(user(CHILD_ID, UserRole.CHILD)));
		given(purchaseRepository.findAllByChildIdAndPeriod(CHILD_ID, null, null))
			.willReturn(List.of(purchase(1, "자전거", 1, 900000)));

		PurchaseHistoryResponse response = purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, PARENT_ID, null,
			null);

		assertThat(response.purchases()).hasSize(1);
	}

	@Test
	@DisplayName("본인 조회인데 그 가족의 자녀가 아니면 FAMILY_NOT_FOUND 예외가 발생한다")
	void getPurchases_selfNotFamilyMember() {
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, CHILD_ID)).willReturn(false);

		assertThatThrownBy(() -> purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, CHILD_ID, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("자녀가 다른 자녀의 구매 내역을 조회하면 FORBIDDEN 예외가 발생한다")
	void getPurchases_childViewsOtherChild() {
		Integer otherChildId = 3;
		given(userRepository.findById(otherChildId)).willReturn(Optional.of(user(otherChildId, UserRole.CHILD)));

		assertThatThrownBy(() -> purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, otherChildId, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("부모가 아닌 사용자가 남의 구매 내역을 조회하려 하면 FORBIDDEN 예외가 발생한다")
	void getPurchases_notParent() {
		given(userRepository.findById(PARENT_ID)).willReturn(Optional.of(user(PARENT_ID, UserRole.CHILD)));

		assertThatThrownBy(() -> purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, PARENT_ID, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("존재하지 않는 사용자가 조회하면 UNAUTHORIZED 예외가 발생한다")
	void getPurchases_userNotFound() {
		given(userRepository.findById(PARENT_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, PARENT_ID, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("부모가 소속되지 않은 가족의 자녀를 조회하면 FORBIDDEN 예외가 발생한다")
	void getPurchases_parentNotFamilyMember() {
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, PARENT_ID)).willReturn(false);
		given(userRepository.findById(PARENT_ID)).willReturn(Optional.of(user(PARENT_ID, UserRole.PARENT)));

		assertThatThrownBy(() -> purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, PARENT_ID, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("대상이 그 가족의 자녀가 아니면 MEMBER_NOT_FOUND 예외가 발생한다")
	void getPurchases_targetNotChildOfFamily() {
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, PARENT_ID)).willReturn(true);
		given(userRepository.findById(PARENT_ID)).willReturn(Optional.of(user(PARENT_ID, UserRole.PARENT)));
		given(familyMemberRepository.existsActiveMember(FAMILY_ID, CHILD_ID)).willReturn(false);

		assertThatThrownBy(() -> purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, PARENT_ID, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("조회 시작일이 종료일보다 늦으면 INVALID_DATE_RANGE 예외가 발생한다")
	void getPurchases_invalidDateRange() {
		LocalDate start = LocalDate.of(2026, 8, 28);
		LocalDate end = LocalDate.of(2026, 8, 1);

		assertThatThrownBy(() -> purchaseHistoryService.getPurchases(FAMILY_ID, CHILD_ID, CHILD_ID, start, end))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_DATE_RANGE);
	}
}
