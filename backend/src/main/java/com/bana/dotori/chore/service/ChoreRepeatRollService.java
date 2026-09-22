package com.bana.dotori.chore.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.chore.entity.ChoreCompletionRequest;
import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.repository.ChoreCompletionRequestRepository;
import com.bana.dotori.chore.repository.ChoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 반복 할 일의 기간을 다음 주기로 미는 일 (크론이 부른다).
 *
 * <p>반복 할 일은 행을 새로 만들지 않고 같은 행의 기간만 갱신한다. 주기마다 행을 만들면
 * 매일 반복 하나가 1년에 365행이 되고, 완료 이력이 참조하는 chore_id 가 주기마다 달라져
 * "이 할 일을 몇 번 했나"를 세려면 행을 다 이어 붙여야 한다.
 *
 * <p>대신 "이번 주기에 이미 했나"는 완료 이력의 시각을 할 일의 기간과 견주어 판단한다
 * (CHORE-09 승인에서 쓴다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChoreRepeatRollService {

	private final ChoreRepository choreRepository;
	private final ChoreCompletionRequestRepository completionRequestRepository;

	/**
	 * 그 주기의 할 일 중 기간이 끝난 것을 오늘 기준 새 주기로 민다.
	 *
	 * @return 기간이 갱신된 할 일 수
	 */
	@Transactional
	public int roll(ChoreRepeatType repeatType, LocalDate today) {
		if (!repeatType.isRepeating()) {
			// 일회성의 기간을 크론이 건드리면 부모가 정한 날짜가 조용히 바뀐다.
			throw new IllegalArgumentException("반복 주기가 아닙니다: " + repeatType);
		}

		// 기간을 밀기 전에 지난 주기의 대기 요청부터 정리한다. 남겨 두면 그 할 일은 새 주기가
		// 와도 잠긴 채로 있고(PENDING_REQUEST_EXISTS), 부모가 나중에 승인하면 지난 주기에 한
		// 일이 이번 주기의 보상으로 지급된다.
		int expired = expireStalePending(repeatType, today);

		int rolled = choreRepository.rollPeriod(repeatType, today, repeatType.endDateFrom(today),
			OffsetDateTime.now());
		if (rolled > 0) {
			log.info("반복 할 일 기간 갱신: repeatType={}, count={}, expired={}, period={}~{}",
				repeatType, rolled, expired, today, repeatType.endDateFrom(today));
		}
		return rolled;
	}

	/**
	 * 주기가 끝나도록 부모가 처리하지 않은 요청을 거절 처리한다.
	 *
	 * <p>거절로 두는 이유는 아이 쪽에서 "다시 하면 된다"로 읽히기 때문이다. 취소(CANCELLED)는
	 * 아이가 스스로 물린 것을 뜻해서, 아이가 하지 않은 일을 아이가 한 것처럼 기록된다.
	 *
	 * @return 거절 처리된 요청 수
	 */
	private int expireStalePending(ChoreRepeatType repeatType, LocalDate today) {
		List<ChoreCompletionRequest> stale = completionRequestRepository.findStalePending(repeatType, today);
		if (stale.isEmpty()) {
			return 0;
		}

		OffsetDateTime now = OffsetDateTime.now();
		for (ChoreCompletionRequest request : stale) {
			request.reject(now);
		}
		log.info("기간이 지난 완료 요청 자동 거절: repeatType={}, count={}", repeatType, stale.size());
		return stale.size();
	}
}
