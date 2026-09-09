package com.bana.dotori.chore.dto;

import java.time.LocalDate;

import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 집안일 생성 요청 (CHORE-04).
 *
 * <p>화면의 "반복" 체크가 repeatType 으로 온다.
 * <ul>
 *   <li>체크 안 함 → {@code ONCE}. 부모가 startDate·endDate 를 직접 넣는다.</li>
 *   <li>체크 함 → {@code DAY}/{@code WEEK}/{@code MONTH}. 날짜는 보내지 않아도 되고,
 *       보내도 무시한다. 시작일은 등록한 날, 종료일은 주기에서 계산한다.</li>
 * </ul>
 *
 * <p>보상은 받지 않는다. AI 가 등록 직후 비동기로 가치 가중치(value)를 산정하고
 * 주간 기본 용돈(families.weekly_allowance)을 곱해 정한다. 부모가 직접 정하게 하면
 * 비현실적인 보상으로 포인트 체계가 무너지므로 의도적으로 막았다
 * (상점 상품의 price 와 같은 기준).
 *
 * <p>childId·familyId 는 경로가 이미 지목하고 있어 본문에 두지 않는다.
 */
public record ChoreCreateRequest(
	@NotBlank @Size(max = 100) String title,
	@Size(max = 255) String description,
	@NotNull ChoreDifficulty difficulty,
	@NotNull ChoreRepeatType repeatType,
	LocalDate startDate,
	LocalDate endDate
) {

	/**
	 * 일회성이면 기간을 반드시 받아야 한다.
	 *
	 * <p>반복은 주기에서 계산하지만 일회성은 계산할 근거가 없다. 비워 두면
	 * "언제부터 언제까지인지 아무도 모르는 할 일"이 만들어진다.
	 */
	@AssertTrue(message = "반복하지 않는 할 일은 시작일과 종료일이 필요합니다")
	public boolean isOncePeriodPresent() {
		if (repeatType == null || repeatType.isRepeating()) {
			return true;
		}
		return startDate != null && endDate != null;
	}

	/**
	 * 일회성의 종료일이 시작일보다 앞설 수 없다.
	 *
	 * <p>반복은 계산으로 나오므로 뒤집힐 수 없어 검사하지 않는다.
	 */
	@AssertTrue(message = "종료일은 시작일과 같거나 이후여야 합니다")
	public boolean isOncePeriodOrdered() {
		if (repeatType == null || repeatType.isRepeating() || startDate == null || endDate == null) {
			return true;
		}
		return !endDate.isBefore(startDate);
	}
}
