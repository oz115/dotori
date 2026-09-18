package com.bana.dotori.report.service;

import org.springframework.stereotype.Component;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;

import lombok.RequiredArgsConstructor;

/**
 * 리포트를 읽을 수 있는 사람인지 확인한다 (RPT 공통).
 *
 * <p>허용되는 경우는 둘이다.
 *
 * <ul>
 *   <li>대상이 호출자 본인이고, 그 가족의 자녀인 경우</li>
 *   <li>호출자가 그 가족의 부모이고, 대상이 같은 가족의 자녀인 경우</li>
 * </ul>
 *
 * <p>명세의 사용자 칸은 "부모"로만 적혀 있지만 본인 조회도 연다. 아이가 자기 리포트를
 * 보는 화면이 생겼을 때 백엔드를 다시 고치지 않아도 되고, 무엇보다 <b>같은 원장을 읽는
 * WLT-04·06 이 이미 이 규칙</b>이라 판정을 다르게 두면 한쪽은 막혔는데 다른 쪽으로 같은
 * 정보가 새어 나가는 구멍이 생긴다.
 *
 * <p>대상이 본인이어도 가족 소속을 확인한다. 경로의 familyId 는 클라이언트가 정하는
 * 값이라, 확인하지 않으면 남의 가족 번호를 끼워 넣어도 200 이 나가 가족 구성을 추측할 수 있다.
 *
 * <p><b>이 규칙은 {@code PointWalletQueryService.validateReadAccess} 와 같은 내용이다.</b>
 * 합치지 않은 이유는 에러 코드가 다르기 때문이다 — 명세가 RPT 는 {@code NOT_FAMILY_CHILD},
 * WLT 는 {@code WALLET_ACCESS_DENIED} 로 정해 두었다(둘 다 403). {@link FamilyMembershipReader}
 * 가 가족 도메인으로 옮겨갈 때(그쪽 javadoc 참고) 판정도 한 곳으로 모으는 것이 맞다.
 */
@Component
@RequiredArgsConstructor
public class ReportAccessValidator {

	private final FamilyMembershipReader familyMembershipReader;

	/**
	 * @throws BusinessException NOT_FAMILY_CHILD 읽을 권한이 없는 경우
	 */
	public void validate(Integer familyId, Integer childId, Integer loginUserId) {
		if (childId == null || loginUserId == null) {
			throw new BusinessException(ErrorCode.NOT_FAMILY_CHILD,
					"조회 대상 또는 호출자를 알 수 없습니다. childId=" + childId + ", loginUserId=" + loginUserId);
		}

		if (!childId.equals(loginUserId)
				&& !familyMembershipReader.isParentOfFamily(familyId, loginUserId)) {
			throw new BusinessException(ErrorCode.NOT_FAMILY_CHILD,
					"남의 리포트는 그 가족의 부모만 조회할 수 있습니다. familyId=" + familyId
							+ ", loginUserId=" + loginUserId + ", childId=" + childId);
		}

		if (!familyMembershipReader.isChildOfFamily(familyId, childId)) {
			throw new BusinessException(ErrorCode.NOT_FAMILY_CHILD,
					"조회 대상이 해당 가족의 자녀가 아닙니다. familyId=" + familyId + ", childId=" + childId);
		}
	}
}
