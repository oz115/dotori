package com.bana.dotori.wallet.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.response.PointBalanceResponse;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;
import com.bana.dotori.wallet.repository.PointWalletRepository;

import lombok.RequiredArgsConstructor;

/**
 * 지갑을 읽기만 하는 통로. 잔액(WLT-04)과 거래내역(WLT-06)이 여기를 거친다.
 *
 * <p>잔액을 바꾸는 {@link PointWalletService} 와 분리한 이유는 두 가지다.
 *
 * <ol>
 *   <li>조회는 락을 잡지 않는다. 변경 경로와 같은 클래스에 두면 실수로
 *       {@code findByUserIdForUpdate} 를 조회에 쓰거나 그 반대가 되기 쉽다.</li>
 *   <li>권한 규칙이 다르다. 충전·지급은 "본인 또는 그 가족 부모가 잔액을 바꾼다"지만
 *       조회는 부모가 자녀 지갑을 들여다볼 수 있다.</li>
 * </ol>
 *
 * <p>조회 권한 판정은 {@link #validateReadAccess} 한 곳에만 둔다.
 * 잔액과 거래내역이 서로 다른 판정을 내리면, 잔액은 막혔는데 거래내역으로는
 * 같은 정보가 새어 나가는 구멍이 생긴다.
 */
@Service
@RequiredArgsConstructor
public class PointWalletQueryService {

	private final PointWalletRepository pointWalletRepository;
	private final FamilyMembershipReader familyMembershipReader;

	/**
	 * WLT-04 보유 포인트 조회.
	 *
	 * @param familyId    경로의 가족
	 * @param childId     조회 대상 자녀
	 * @param loginUserId 호출자
	 * @return 잔액. 지갑이 아직 없으면 0
	 */
	@Transactional(readOnly = true)
	public PointBalanceResponse getBalance(Integer familyId, Integer childId, Integer loginUserId) {
		validateReadAccess(familyId, childId, loginUserId);

		return pointWalletRepository.findByUserId(childId)
				.map(PointBalanceResponse::from)
				.orElseGet(() -> PointBalanceResponse.empty(childId));
	}

	/**
	 * 호출자가 이 자녀의 지갑을 읽을 수 있는지 확인한다.
	 *
	 * <p>허용되는 경우는 둘뿐이다.
	 *
	 * <ul>
	 *   <li>대상이 호출자 본인이고, 그 가족의 자녀인 경우</li>
	 *   <li>호출자가 그 가족의 부모이고, 대상이 같은 가족의 자녀인 경우</li>
	 * </ul>
	 *
	 * <p>대상이 본인이어도 가족 소속을 확인한다. 경로의 familyId 는 클라이언트가 정하는 값이라,
	 * 확인하지 않으면 남의 가족 번호를 끼워 넣어도 200 이 나가 가족 구성을 추측할 수 있다.
	 *
	 * @throws BusinessException WALLET_ACCESS_DENIED 읽을 권한이 없는 경우
	 */
	public void validateReadAccess(Integer familyId, Integer childId, Integer loginUserId) {
		if (childId == null || loginUserId == null) {
			throw new BusinessException(ErrorCode.WALLET_ACCESS_DENIED,
					"조회 대상 또는 호출자를 알 수 없습니다. childId=" + childId + ", loginUserId=" + loginUserId);
		}

		if (childId.equals(loginUserId)) {
			if (!familyMembershipReader.isChildOfFamily(familyId, childId)) {
				throw new BusinessException(ErrorCode.WALLET_ACCESS_DENIED,
						"본인 조회이지만 해당 가족의 자녀가 아닙니다. familyId=" + familyId + ", childId=" + childId);
			}
			return;
		}

		// 남의 지갑을 보려면 그 가족의 부모여야 한다.
		if (!familyMembershipReader.isParentOfFamily(familyId, loginUserId)) {
			throw new BusinessException(ErrorCode.WALLET_ACCESS_DENIED,
					"남의 지갑은 그 가족의 부모만 조회할 수 있습니다. familyId=" + familyId
							+ ", loginUserId=" + loginUserId + ", childId=" + childId);
		}

		if (!familyMembershipReader.isChildOfFamily(familyId, childId)) {
			throw new BusinessException(ErrorCode.WALLET_ACCESS_DENIED,
					"조회 대상이 해당 가족의 자녀가 아닙니다. familyId=" + familyId + ", childId=" + childId);
		}
	}
}
