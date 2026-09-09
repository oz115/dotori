package com.bana.dotori.chore.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.chore.dto.ChoreCompletionHistoryItem;
import com.bana.dotori.chore.dto.ChoreCompletionHistoryResponse;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.repository.UserRepository;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.repository.PointTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChoreCompletionHistoryServiceImpl implements ChoreCompletionHistoryService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/** 기간을 지정하지 않았을 때 보여 줄 날 수 (오늘 포함). */
	private static final int DEFAULT_PERIOD_DAYS = 30;

	/**
	 * 한 번에 돌려줄 최대 건수.
	 *
	 * <p>명세에 페이징이 없어 기간 전체를 한 응답에 담는다. 상한이 없으면 긴 기간을 물었을 때
	 * 응답이 얼마나 커질지 알 수 없으므로 잘라 둔다. 기본 30일이면 닿을 일이 없고,
	 * 여기 걸릴 만큼 기간을 넓게 쓰는 화면이 생기면 그때 커서 페이징을 붙인다
	 * (지갑 거래내역이 같은 이유로 커서를 쓴다).
	 */
	private static final int MAX_ITEMS = 500;

	private final PointTransactionRepository pointTransactionRepository;
	private final ChoreRepository choreRepository;
	private final UserRepository userRepository;
	private final ChoreAccessValidator accessValidator;

	/**
	 * 기간 안에 실제로 지급된 집안일 보상을 모은다 (CHORE-03).
	 *
	 * <p>부모와 아이가 모두 호출한다. 경로가 아이를 지목하므로 아이는 자기 것만 볼 수 있고
	 * (접근 검증이 형제를 막는다), 부모는 자녀를 골라 본다.
	 *
	 * <p>지갑 원장을 읽는 이유는 금액 때문이다. 완료 요청 행에는 지급액이 없고 승인 시점의
	 * {@code chores.reward_points} 를 읽으면, 부모가 그 뒤 보상을 고쳤을 때 과거 이력이
	 * 통째로 바뀌어 지갑 거래내역과 다른 숫자를 보여 준다.
	 *
	 * <p>거르는 순서가 중요하다. 먼저 {@code reason = CHORE_REWARD} 로 좁힌 뒤에만 멱등키를
	 * 해석한다. 충전·지급은 멱등키를 <b>클라이언트가 정하므로</b> 누가
	 * {@code chore:1:2026-01-01} 을 충전 키로 보내면, 키 모양만 보고 거르는 코드는 남의 충전을
	 * 집안일 보상으로 착각한다.
	 */
	@Override
	@Transactional(readOnly = true)
	public ChoreCompletionHistoryResponse getCompletions(Integer userId, Integer familyId, Integer childId,
			LocalDate startDate, LocalDate endDate) {
		accessValidator.validate(userId, familyId, childId);

		LocalDate to = endDate != null ? endDate : LocalDate.now(KST);
		LocalDate from = startDate != null ? startDate : to.minusDays(DEFAULT_PERIOD_DAYS - 1L);
		if (from.isAfter(to)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE,
					"조회 시작일이 종료일보다 늦습니다. startDate=" + from + ", endDate=" + to);
		}

		List<PointTransaction> paid = pointTransactionRepository.search(childId,
				startOfDay(from), startOfNextDay(to), PointTransactionReason.CHORE_REWARD,
				null, Sort.Direction.DESC, MAX_ITEMS);

		String childName = userRepository.findById(childId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
				.getName();

		return new ChoreCompletionHistoryResponse(toItems(paid, childName));
	}

	/**
	 * 원장 행에 집안일 제목을 붙인다.
	 *
	 * <p>제목은 거래에 없고 멱등키 안의 집안일 ID 로만 찾을 수 있다. 건마다 조회하면
	 * 한 화면에 쿼리가 수백 번 나가므로 ID 를 모아 한 번에 읽는다.
	 *
	 * <p>지워진 집안일도 그대로 나온다. 이미 받은 보상의 근거라 이력에서 빠지면 안 된다
	 * (소프트 삭제라 행은 남아 있다).
	 */
	private List<ChoreCompletionHistoryItem> toItems(List<PointTransaction> paid, String childName) {
		List<Integer> choreIds = paid.stream()
				.map(transaction -> Chore.choreIdFrom(transaction.getIdempotencyKey()))
				.filter(Objects::nonNull)
				.distinct()
				.toList();

		Map<Integer, String> titles = choreRepository.findAllById(choreIds).stream()
				.collect(Collectors.toMap(Chore::getId, Chore::getTitle));

		return paid.stream()
				.map(transaction -> toItem(transaction, childName, titles))
				.toList();
	}

	private ChoreCompletionHistoryItem toItem(PointTransaction transaction, String childName,
			Map<Integer, String> titles) {
		Integer choreId = Chore.choreIdFrom(transaction.getIdempotencyKey());
		return new ChoreCompletionHistoryItem(choreId, titles.get(choreId), childName,
				transaction.getAmount(), transaction.getCreatedAt());
	}

	/** 조회 시작일 00:00:00 KST. */
	private OffsetDateTime startOfDay(LocalDate date) {
		return date.atStartOfDay(KST).toOffsetDateTime();
	}

	/**
	 * 조회 종료일 다음 날 00:00:00 KST.
	 *
	 * <p>종료일을 포함하려면 반개구간의 끝을 다음 날 0시로 잡아야 한다. {@code created_at} 이
	 * TIMESTAMPTZ 라 "종료일 23:59:59" 로 자르면 그 뒤 밀리초에 생긴 거래가 빠진다.
	 * 지갑 거래내역(WLT-06)이 같은 방식이다.
	 */
	private OffsetDateTime startOfNextDay(LocalDate date) {
		return date.plusDays(1).atStartOfDay(KST).toOffsetDateTime();
	}
}
