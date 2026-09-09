package com.bana.dotori.chore.dto;

import java.time.LocalDate;

import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 집안일 수정 요청 (CHORE-05). PATCH 이므로 보낸 필드만 바뀐다.
 *
 * <p>모든 필드가 선택이다. {@code @NotBlank} 를 쓸 수 없는 이유는 그 제약이 null 도 거부해서
 * "이 필드는 안 바꿈"을 표현할 수 없기 때문이다. 대신 {@code @Pattern} 으로 공백만 막았다 —
 * {@code @Pattern} 과 {@code @Size} 는 null 을 통과시킨다.
 *
 * <p>childId 는 받지 않는다. 담당을 옮기면 이미 쌓인 완료 이력이 누구 것인지 모호해진다.
 * 담당을 바꾸려면 기존 것을 비활성화하고 새로 만든다.
 *
 * <p>기간 순서는 여기서 검증하지 않는다. startDate 만 보내면 종료일의 현재 값을 알 수 없어
 * 판정이 불가능하다. 합쳐진 최종 상태를 아는 {@code Chore.update} 가 본다.
 */
public record ChoreUpdateRequest(
	@Pattern(regexp = ".*\\S.*", message = "제목은 공백일 수 없습니다")
	@Size(max = 100) String title,
	@Size(max = 255) String description,
	ChoreDifficulty difficulty,
	@Min(1) Integer rewardPoints,
	ChoreRepeatType repeatType,
	LocalDate startDate,
	LocalDate endDate
) {
}
