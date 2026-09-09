package com.bana.dotori.chore.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.chore.entity.ChoreRepeatType;
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

		int rolled = choreRepository.rollPeriod(repeatType, today, repeatType.endDateFrom(today),
			OffsetDateTime.now());
		if (rolled > 0) {
			log.info("반복 할 일 기간 갱신: repeatType={}, count={}, period={}~{}",
				repeatType, rolled, today, repeatType.endDateFrom(today));
		}
		return rolled;
	}
}
