package com.bana.dotori.store.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.store.dto.PurchaseHistoryItem;
import com.bana.dotori.store.dto.PurchaseHistoryResponse;
import com.bana.dotori.store.entity.Purchase;
import com.bana.dotori.store.repository.PurchaseRepository;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * SHOP-07 구매 내역 조회.
 *
 * <p>조회 권한은 지갑 거래내역(WLT-06)과 같은 규칙이다 — 아이는 본인, 부모는 같은 가족 자녀.
 * 대상이 본인이어도 가족 소속을 확인한다. 경로의 familyId 는 클라이언트가 정하는 값이라,
 * 확인하지 않으면 남의 가족 번호를 끼워 넣어도 200 이 나가 가족 구성을 추측할 수 있다.
 */
@Service
@RequiredArgsConstructor
public class PurchaseHistoryService {

	private final PurchaseRepository purchaseRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public PurchaseHistoryResponse getPurchases(Integer familyId, Integer childId, Integer loginUserId,
			LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
		}
		validateReadAccess(familyId, childId, loginUserId);

		List<Purchase> purchases = purchaseRepository.findAllByChildIdAndPeriod(childId, startDate, endDate);
		return new PurchaseHistoryResponse(purchases.stream().map(this::toItem).toList());
	}

	private PurchaseHistoryItem toItem(Purchase purchase) {
		return new PurchaseHistoryItem(purchase.getId(), purchase.getProductName(), purchase.getQuantity(),
			purchase.getPrice() * purchase.getQuantity(), purchase.getPurchasedAt());
	}

	/**
	 * 호출자가 이 자녀의 구매 내역을 읽을 수 있는지 확인한다.
	 *
	 * <p>허용되는 경우는 둘뿐이다.
	 * <ul>
	 *   <li>대상이 호출자 본인이고, 그 가족의 자녀인 경우</li>
	 *   <li>호출자가 그 가족의 부모이고, 대상이 같은 가족의 자녀인 경우</li>
	 * </ul>
	 */
	private void validateReadAccess(Integer familyId, Integer childId, Integer loginUserId) {
		if (childId.equals(loginUserId)) {
			if (!isActiveChildOfFamily(familyId, loginUserId)) {
				throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
			}
			return;
		}

		User loginUser = userRepository.findById(loginUserId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (loginUser.getRole() != UserRole.PARENT || !familyMemberRepository.existsActiveMember(familyId, loginUserId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (!isActiveChildOfFamily(familyId, childId)) {
			throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
		}
	}

	private boolean isActiveChildOfFamily(Integer familyId, Integer userId) {
		if (!familyMemberRepository.existsActiveMember(familyId, userId)) {
			return false;
		}
		return userRepository.findById(userId)
			.map(user -> user.getRole() == UserRole.CHILD)
			.orElse(false);
	}
}
