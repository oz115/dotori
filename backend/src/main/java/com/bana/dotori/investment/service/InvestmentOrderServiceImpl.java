package com.bana.dotori.investment.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentOrderRequest;
import com.bana.dotori.investment.dto.InvestmentOrderResponse;
import com.bana.dotori.investment.entity.InvestmentHolding;
import com.bana.dotori.investment.entity.InvestmentPriceHistory;
import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.entity.InvestmentTransaction;
import com.bana.dotori.investment.entity.InvestmentTransactionType;
import com.bana.dotori.investment.mapper.InvestmentProductMapper;
import com.bana.dotori.investment.repository.InvestmentHoldingRepository;
import com.bana.dotori.investment.repository.InvestmentPriceHistoryRepository;
import com.bana.dotori.investment.repository.InvestmentProductRepository;
import com.bana.dotori.investment.repository.InvestmentTransactionRepository;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointDeductionService;

import lombok.RequiredArgsConstructor;

/**
 * 매수/매도 주문 처리. 조회(InvestmentProductService)와 달리 돈이 움직이는 쓰기 경로라
 * 별도 서비스로 뒀다. 한 트랜잭션 안에서 포인트 이동 + 거래 기록 + 보유 행 갱신이
 * 함께 커밋되거나 함께 롤백된다.
 *
 * <p>종목당 1주 규칙: 매수는 미보유 종목만 1주, 매도는 들고 있는 1주 전량.
 * 체결은 시장가 — 체결가는 그 가족의 오늘 현재가(이력 없으면 시작가)다.
 */
@Service
@RequiredArgsConstructor
public class InvestmentOrderServiceImpl implements InvestmentOrderService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int SINGLE_SHARE = 1;

	private final InvestmentProductRepository investmentProductRepository;
	private final InvestmentPriceHistoryRepository investmentPriceHistoryRepository;
	private final InvestmentHoldingRepository investmentHoldingRepository;
	private final InvestmentTransactionRepository investmentTransactionRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final InvestmentProductMapper investmentProductMapper;
	private final PointDeductionService pointDeductionService;

	@Override
	@Transactional
	public InvestmentOrderResponse buy(Integer userId, InvestmentOrderRequest request) {
		Integer familyId = requireChildFamily(userId);

		InvestmentProduct product = investmentProductRepository.findById(request.investmentProductId())
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		if (!product.getIsActive()) {
			throw new BusinessException(ErrorCode.PRODUCT_INACTIVE);
		}
		if (investmentHoldingRepository.existsByChildIdAndInvestmentProductId(userId, product.getId())) {
			throw new BusinessException(ErrorCode.ALREADY_HOLDING);
		}

		int unitPrice = currentPrice(familyId, product);

		// 잔액 부족이면 여기서 INSUFFICIENT_POINT 로 끝난다 — 거래·보유 행이 생기지 않는다
		int balanceAfter = pointDeductionService.deduct(userId, unitPrice,
			PointTransactionReason.INVESTMENT_BUY, "투자 매수: " + product.getName(), null).balanceAfter();

		InvestmentTransaction transaction = investmentTransactionRepository.save(
			transaction(familyId, userId, product.getId(), InvestmentTransactionType.BUY, unitPrice));
		try {
			// 검사와 INSERT 사이의 연타는 유니크(child_id, product_id)가 막는다.
			// 위반이면 이 트랜잭션이 통째로 롤백되어 위의 포인트 차감도 되돌아간다.
			investmentHoldingRepository.saveAndFlush(InvestmentHolding.builder()
				.familyId(familyId)
				.childId(userId)
				.investmentProductId(product.getId())
				.quantity(SINGLE_SHARE)
				.buyPrice(unitPrice)
				.build());
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(ErrorCode.ALREADY_HOLDING);
		}

		return investmentProductMapper.toOrderResponse(transaction, balanceAfter);
	}

	/** 매수/매도는 아이 전용 — 부모는 자녀의 투자를 보기만 한다. */
	private Integer requireChildFamily(Integer userId) {
		Integer familyId = familyMemberRepository.findActiveFamilyId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		FamilyMember member = familyMemberRepository.findWithUserByFamilyIdAndUserId(familyId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		if (member.getUser().getRole() != UserRole.CHILD) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "아이만 투자할 수 있습니다. userId=" + userId);
		}
		return familyId;
	}

	/** 체결가 = 그 가족의 오늘 현재가 (조회 API 들과 같은 규칙, 이력이 없으면 시작가). */
	private int currentPrice(Integer familyId, InvestmentProduct product) {
		OffsetDateTime tomorrowStart = LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime().plusDays(1);
		return investmentPriceHistoryRepository
			.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
				familyId, product.getId(), tomorrowStart)
			.map(InvestmentPriceHistory::getPrice)
			.orElse(product.getCurrentPrice());
	}

	private InvestmentTransaction transaction(Integer familyId, Integer childId, Integer productId,
		InvestmentTransactionType type, int unitPrice) {
		return InvestmentTransaction.builder()
			.familyId(familyId)
			.childId(childId)
			.investmentProductId(productId)
			.transactionType(type)
			.quantity(SINGLE_SHARE)
			.unitPrice(unitPrice)
			.totalAmount(unitPrice * SINGLE_SHARE)
			.createdAt(OffsetDateTime.now())
			.build();
	}
}
