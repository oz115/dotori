package com.bana.dotori.chore.dto;

import java.time.LocalDate;

import com.bana.dotori.chore.entity.ChoreRepeatType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 집안일 생성 요청 (CHORE-04).
 *
 * <p>childId·familyId 는 본문에 없다. 경로가 이미 지목하고 있어 두 군데서 받으면
 * 서로 다른 값이 올 때 무엇을 믿을지 정해야 한다. parentId 도 인증 주체에서 채운다.
 *
 * <p>startDate 를 비우면 오늘부터, endDate 를 비우면 기한 없이 반복한다.
 */
public record ChoreCreateRequest(
	@NotBlank @Size(max = 100) String title,
	@Size(max = 255) String description,
	@NotNull @Min(1) Integer rewardPoints,
	@NotNull ChoreRepeatType repeatType,
	LocalDate startDate,
	LocalDate endDate
) {

	/**
	 * 종료일이 시작일보다 앞서면 애초에 수행할 수 없는 집안일이 된다.
	 *
	 * <p>필드 하나만 보는 제약으로는 잡을 수 없어 요청 전체를 보고 판정한다.
	 * startDate 를 비운 경우는 엔티티가 오늘로 채우므로 여기서도 오늘로 보고 비교한다.
	 */
	@AssertTrue(message = "종료일은 시작일과 같거나 이후여야 합니다")
	public boolean isPeriodOrdered() {
		if (endDate == null) {
			return true;
		}
		LocalDate from = startDate != null ? startDate : LocalDate.now();
		return !endDate.isBefore(from);
	}
}
