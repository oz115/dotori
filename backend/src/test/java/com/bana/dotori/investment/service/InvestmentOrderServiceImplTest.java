package com.bana.dotori.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentOrderRequest;
import com.bana.dotori.investment.dto.InvestmentOrderResponse;
import com.bana.dotori.investment.entity.InvestmentHolding;
import com.bana.dotori.investment.entity.InvestmentPriceHistory;
import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.entity.InvestmentProductCategory;
import com.bana.dotori.investment.entity.InvestmentTransaction;
import com.bana.dotori.investment.entity.InvestmentTransactionType;
import com.bana.dotori.investment.mapper.InvestmentProductMapper;
import com.bana.dotori.investment.repository.InvestmentHoldingRepository;
import com.bana.dotori.investment.repository.InvestmentPriceHistoryRepository;
import com.bana.dotori.investment.repository.InvestmentProductRepository;
import com.bana.dotori.investment.repository.InvestmentTransactionRepository;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointDeductionService;
import com.bana.dotori.wallet.service.PointWalletService;

@ExtendWith(MockitoExtension.class)
class InvestmentOrderServiceImplTest {

	private static final Integer CHILD_ID = 20;
	private static final Integer FAMILY_ID = 7;

	@Mock
	private InvestmentProductRepository investmentProductRepository;

	@Mock
	private InvestmentPriceHistoryRepository investmentPriceHistoryRepository;

	@Mock
	private InvestmentHoldingRepository investmentHoldingRepository;

	@Mock
	private InvestmentTransactionRepository investmentTransactionRepository;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Mock
	private PointDeductionService pointDeductionService;

	@Mock
	private PointWalletService pointWalletService;

	@Spy
	private InvestmentProductMapper investmentProductMapper;

	@InjectMocks
	private InvestmentOrderServiceImpl investmentOrderService;

	private InvestmentProduct product(Integer id, boolean active) {
		InvestmentProduct product = InvestmentProduct.builder()
			.name("시원아이스")
			.category(InvestmentProductCategory.STOCK)
			.currentPrice(10000)
			.isActive(active)
			.updatedAt(OffsetDateTime.parse("2026-09-01T00:00:00+09:00"))
			.build();
		ReflectionTestUtils.setField(product, "id", id);
		return product;
	}

	private void givenChild() {
		given(familyMemberRepository.findActiveFamilyId(CHILD_ID)).willReturn(Optional.of(FAMILY_ID));
		FamilyMember member = FamilyMember.builder()
			.user(User.builder().loginId("kid").name("아이").email("kid@test.local").role(UserRole.CHILD).build())
			.build();
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, CHILD_ID))
			.willReturn(Optional.of(member));
	}

	private void givenCurrentPrice(Integer productId, Integer price) {
		Optional<InvestmentPriceHistory> row = price == null ? Optional.empty()
			: Optional.of(InvestmentPriceHistory.builder()
				.familyId(FAMILY_ID)
				.investmentProductId(productId)
				.price(price)
				.recordedAt(OffsetDateTime.now())
				.build());
		given(investmentPriceHistoryRepository
			.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
				eq(FAMILY_ID), eq(productId), any(OffsetDateTime.class)))
			.willReturn(row);
	}

	private void givenDeduction(int amount, int balanceAfter) {
		given(pointDeductionService.deduct(eq(CHILD_ID), eq(amount),
			eq(PointTransactionReason.INVESTMENT_BUY), anyString(), isNull()))
			.willReturn(new PointChangeResult(1, CHILD_ID, PointTransactionReason.INVESTMENT_BUY, amount,
				balanceAfter, OffsetDateTime.now(), false));
	}

	private void givenTransactionSaved() {
		given(investmentTransactionRepository.save(any(InvestmentTransaction.class))).willAnswer(invocation -> {
			InvestmentTransaction transaction = invocation.getArgument(0);
			ReflectionTestUtils.setField(transaction, "id", 100);
			return transaction;
		});
	}

	@Test
	@DisplayName("매수 성공 시 현재가로 차감·거래 기록·보유 행을 만들고 체결 결과를 반환한다")
	void buy_success() {
		givenChild();
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, true)));
		given(investmentHoldingRepository.existsByChildIdAndInvestmentProductId(CHILD_ID, 1)).willReturn(false);
		givenCurrentPrice(1, 1200);
		givenDeduction(1200, 8000);
		givenTransactionSaved();

		InvestmentOrderResponse response = investmentOrderService.buy(CHILD_ID, new InvestmentOrderRequest(1));

		assertThat(response.investmentTransactionId()).isEqualTo(100);
		assertThat(response.transactionType()).isEqualTo(InvestmentTransactionType.BUY);
		assertThat(response.quantity()).isEqualTo(1);
		assertThat(response.unitPrice()).isEqualTo(1200);
		assertThat(response.totalAmount()).isEqualTo(1200);
		assertThat(response.pointBalanceAfter()).isEqualTo(8000);

		ArgumentCaptor<InvestmentHolding> holdingCaptor = ArgumentCaptor.forClass(InvestmentHolding.class);
		then(investmentHoldingRepository).should().saveAndFlush(holdingCaptor.capture());
		InvestmentHolding holding = holdingCaptor.getValue();
		assertThat(holding.getChildId()).isEqualTo(CHILD_ID);
		assertThat(holding.getQuantity()).isEqualTo(1);
		assertThat(holding.getBuyPrice()).isEqualTo(1200);
	}

	@Test
	@DisplayName("가격 이력이 없으면 시작가로 체결한다")
	void buy_noHistoryUsesSeedPrice() {
		givenChild();
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, true)));
		given(investmentHoldingRepository.existsByChildIdAndInvestmentProductId(CHILD_ID, 1)).willReturn(false);
		givenCurrentPrice(1, null);
		givenDeduction(10000, 0);
		givenTransactionSaved();

		InvestmentOrderResponse response = investmentOrderService.buy(CHILD_ID, new InvestmentOrderRequest(1));

		assertThat(response.unitPrice()).isEqualTo(10000);
	}

	@Test
	@DisplayName("부모가 매수하면 FORBIDDEN 예외가 발생하고 포인트는 움직이지 않는다")
	void buy_parentForbidden() {
		given(familyMemberRepository.findActiveFamilyId(CHILD_ID)).willReturn(Optional.of(FAMILY_ID));
		FamilyMember parent = FamilyMember.builder()
			.user(User.builder().loginId("mom").name("부모").email("mom@test.local").role(UserRole.PARENT).build())
			.build();
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, CHILD_ID))
			.willReturn(Optional.of(parent));

		assertThatThrownBy(() -> investmentOrderService.buy(CHILD_ID, new InvestmentOrderRequest(1)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FORBIDDEN);
		then(pointDeductionService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("없는 종목을 매수하면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void buy_productNotFound() {
		givenChild();
		given(investmentProductRepository.findById(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> investmentOrderService.buy(CHILD_ID, new InvestmentOrderRequest(99)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("거래 중지된 종목을 매수하면 PRODUCT_INACTIVE 예외가 발생하고 포인트는 움직이지 않는다")
	void buy_inactiveProduct() {
		givenChild();
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, false)));

		assertThatThrownBy(() -> investmentOrderService.buy(CHILD_ID, new InvestmentOrderRequest(1)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PRODUCT_INACTIVE);
		then(pointDeductionService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("이미 보유 중인 종목을 매수하면 ALREADY_HOLDING 예외가 발생하고 포인트는 움직이지 않는다")
	void buy_alreadyHolding() {
		givenChild();
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, true)));
		given(investmentHoldingRepository.existsByChildIdAndInvestmentProductId(CHILD_ID, 1)).willReturn(true);

		assertThatThrownBy(() -> investmentOrderService.buy(CHILD_ID, new InvestmentOrderRequest(1)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.ALREADY_HOLDING);
		then(pointDeductionService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("매도 성공 시 보유 행을 지우고 현재가만큼 지급한 뒤 체결 결과를 반환한다")
	void sell_success() {
		givenChild();
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, true)));
		given(investmentHoldingRepository.deleteHolding(FAMILY_ID, CHILD_ID, 1)).willReturn(1);
		givenCurrentPrice(1, 1300);
		given(pointWalletService.deposit(any(PointChangeCommand.class)))
			.willReturn(new PointChangeResult(2, CHILD_ID, PointTransactionReason.INVESTMENT_SELL, 1300,
				9300, OffsetDateTime.now(), false));
		givenTransactionSaved();

		InvestmentOrderResponse response = investmentOrderService.sell(CHILD_ID, new InvestmentOrderRequest(1));

		assertThat(response.transactionType()).isEqualTo(InvestmentTransactionType.SELL);
		assertThat(response.quantity()).isEqualTo(1);
		assertThat(response.unitPrice()).isEqualTo(1300);
		assertThat(response.totalAmount()).isEqualTo(1300);
		assertThat(response.pointBalanceAfter()).isEqualTo(9300);

		ArgumentCaptor<PointChangeCommand> commandCaptor = ArgumentCaptor.forClass(PointChangeCommand.class);
		then(pointWalletService).should().deposit(commandCaptor.capture());
		assertThat(commandCaptor.getValue().amount()).isEqualTo(1300);
		assertThat(commandCaptor.getValue().reason()).isEqualTo(PointTransactionReason.INVESTMENT_SELL);
	}

	@Test
	@DisplayName("거래 중지된 종목도 매도는 가능하다 — 아이 포인트가 묶이지 않게")
	void sell_inactiveProductAllowed() {
		givenChild();
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, false)));
		given(investmentHoldingRepository.deleteHolding(FAMILY_ID, CHILD_ID, 1)).willReturn(1);
		givenCurrentPrice(1, 900);
		given(pointWalletService.deposit(any(PointChangeCommand.class)))
			.willReturn(new PointChangeResult(2, CHILD_ID, PointTransactionReason.INVESTMENT_SELL, 900,
				900, OffsetDateTime.now(), false));
		givenTransactionSaved();

		InvestmentOrderResponse response = investmentOrderService.sell(CHILD_ID, new InvestmentOrderRequest(1));

		assertThat(response.unitPrice()).isEqualTo(900);
	}

	@Test
	@DisplayName("보유하지 않은 종목을 매도하면 INSUFFICIENT_QUANTITY 예외가 발생하고 지급하지 않는다")
	void sell_notHolding() {
		givenChild();
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, true)));
		given(investmentHoldingRepository.deleteHolding(FAMILY_ID, CHILD_ID, 1)).willReturn(0);

		assertThatThrownBy(() -> investmentOrderService.sell(CHILD_ID, new InvestmentOrderRequest(1)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INSUFFICIENT_QUANTITY);
		then(pointWalletService).shouldHaveNoInteractions();
		then(investmentTransactionRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("연타로 보유 유니크 제약에 걸리면 ALREADY_HOLDING 예외로 바뀐다")
	void buy_uniqueViolation() {
		givenChild();
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, true)));
		given(investmentHoldingRepository.existsByChildIdAndInvestmentProductId(CHILD_ID, 1)).willReturn(false);
		givenCurrentPrice(1, 1200);
		givenDeduction(1200, 8000);
		givenTransactionSaved();
		willThrow(new DataIntegrityViolationException("UX_INVESTMENT_HOLDINGS_CHILD_PRODUCT"))
			.given(investmentHoldingRepository).saveAndFlush(any(InvestmentHolding.class));

		assertThatThrownBy(() -> investmentOrderService.buy(CHILD_ID, new InvestmentOrderRequest(1)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.ALREADY_HOLDING);
	}
}
