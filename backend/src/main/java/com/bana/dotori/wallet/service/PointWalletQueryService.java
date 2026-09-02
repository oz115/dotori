package com.bana.dotori.wallet.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.response.PointBalanceResponse;
import com.bana.dotori.wallet.dto.response.PointTransactionHistoryResponse;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;
import com.bana.dotori.wallet.repository.PointTransactionRepository;
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

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/**
	 * 한 번에 가져갈 수 있는 거래 수 상한.
	 * 상한이 없으면 size=1000000 한 방으로 원장 전체를 끌어갈 수 있다.
	 */
	static final int MAX_PAGE_SIZE = 100;

	private final PointWalletRepository pointWalletRepository;
	private final PointTransactionRepository pointTransactionRepository;
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
	 * WLT-06 포인트 거래내역 조회.
	 *
	 * @param startDate 조회 시작일(KST, 포함). null 이면 제한 없음
	 * @param endDate   조회 종료일(KST, 포함). null 이면 제한 없음
	 * @param reason    거래 사유 필터. null 이면 전체
	 * @param direction 거래 시각 정렬 방향
	 * @param page      0부터 시작하는 페이지 번호
	 * @param size      페이지 크기
	 */
	@Transactional(readOnly = true)
	public PointTransactionHistoryResponse getTransactions(Integer familyId, Integer childId, Integer loginUserId,
			LocalDate startDate, LocalDate endDate, PointTransactionReason reason,
			Sort.Direction direction, int page, int size) {

		validateReadAccess(familyId, childId, loginUserId);
		validatePeriod(startDate, endDate);
		validatePageRequest(page, size);

		Optional<PointWallet> wallet = pointWalletRepository.findByUserId(childId);
		if (wallet.isEmpty()) {
			// 지갑이 없으면 원장도 없다. 404 대신 빈 목록으로 응답한다.
			return PointTransactionHistoryResponse.empty(childId, page, size);
		}

		Page<PointTransaction> found = pointTransactionRepository.search(
				wallet.get().getWalletId(),
				startOfDay(startDate),
				startOfNextDay(endDate),
				reason,
				toPageable(direction, page, size));

		return PointTransactionHistoryResponse.of(childId, wallet.get().getBalance(), found);
	}

	/**
	 * 정렬은 거래 시각을 기준으로 하되 마지막에 항상 식별자를 붙인다.
	 *
	 * <p>같은 초에 여러 거래가 생기면(동시 요청·연속 호출) created_at 만으로는 순서가
	 * 고정되지 않는다. 페이지마다 순서가 흔들리면 어떤 거래는 두 번 보이고
	 * 어떤 거래는 아예 빠진다. 원장은 삽입 순서가 곧 시간 순서라
	 * point_transaction_id 를 같은 방향으로 덧붙이면 전순서가 확정된다.
	 */
	private Pageable toPageable(Sort.Direction direction, int page, int size) {
		Sort.Direction resolved = direction == null ? Sort.Direction.DESC : direction;
		return PageRequest.of(page, size, Sort.by(resolved, "createdAt", "pointTransactionId"));
	}

	private void validatePeriod(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE,
					"조회 시작일이 종료일보다 늦습니다. startDate=" + startDate + ", endDate=" + endDate);
		}
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_PAGE_REQUEST,
					"페이지 요청 값이 올바르지 않습니다. page=" + page + ", size=" + size
							+ ", 허용 size=1~" + MAX_PAGE_SIZE);
		}
	}

	/** 조회 시작일 00:00:00 KST. */
	private OffsetDateTime startOfDay(LocalDate date) {
		return date == null ? null : date.atStartOfDay(KST).toOffsetDateTime();
	}

	/**
	 * 조회 종료일 다음 날 00:00:00 KST.
	 *
	 * <p>종료일을 포함하려면 반개구간의 끝을 다음 날 0시로 잡아야 한다.
	 * "종료일 23:59:59" 로 자르면 그 뒤 밀리초에 생긴 거래가 빠진다.
	 */
	private OffsetDateTime startOfNextDay(LocalDate date) {
		return date == null ? null : date.plusDays(1).atStartOfDay(KST).toOffsetDateTime();
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
