package com.bana.dotori.chore.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부모가 아이에게 배정한 집안일 (CHORE).
 *
 * <p>완료 인증은 이 테이블이 아니라 chore_completion_requests 에 쌓인다.
 * 여기 있는 rewardPoints 는 "승인 시점에 지급할 금액"이다. 요청 시점 값을 따로
 * 스냅샷하지 않으므로, 대기 중인 요청이 있는 상태에서 금액을 바꾸면 바뀐 값이 지급된다.
 *
 * <p>삭제는 물리 삭제가 아니라 isActive=false 다. 완료 이력이 choreId 를 참조하기 때문에
 * 행을 지우면 이미 지급한 보상의 근거가 사라진다.
 */
@Entity
@Table(name = "chores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chore {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chore_id")
	private Integer id;

	@Column(name = "family_id", nullable = false, updatable = false)
	private Integer familyId;

	/** 등록한 부모. 담당 아이(childId)와 헷갈리지 않게 주의한다. */
	@Column(name = "parent_id", nullable = false, updatable = false)
	private Integer parentId;

	/**
	 * 담당 아이. 바꿀 수 없다 — 담당을 옮기면 이미 쌓인 완료 이력이 누구 것인지 모호해진다.
	 * 담당을 바꾸려면 이 집안일을 비활성화하고 새로 만든다.
	 */
	@Column(name = "child_id", nullable = false, updatable = false)
	private Integer childId;

	@Column(name = "title", length = 100, nullable = false)
	private String title;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "reward_points", nullable = false)
	private Integer rewardPoints;

	@Enumerated(EnumType.STRING)
	@Column(name = "repeat_type", nullable = false)
	private ChoreRepeatType repeatType;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	/** 없으면 기한 없이 계속 반복한다. */
	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "is_active", nullable = false)
	private boolean isActive;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	/**
	 * startDate 가 없으면 오늘로 둔다.
	 *
	 * <p>DB 에도 DEFAULT CURRENT_DATE 가 걸려 있지만 Hibernate 는 INSERT 에 모든 컬럼을 실으므로
	 * null 을 그대로 두면 DEFAULT 가 아니라 NOT NULL 위반이 된다.
	 * isActive 를 여기서 true 로 세우는 이유도 같다. boolean 기본값 false 가 실려 나가면
	 * 등록하자마자 비활성 상태인 집안일이 만들어진다.
	 */
	@Builder
	private Chore(Integer familyId, Integer parentId, Integer childId, String title, String description,
			Integer rewardPoints, ChoreRepeatType repeatType, LocalDate startDate, LocalDate endDate) {
		this.familyId = familyId;
		this.parentId = parentId;
		this.childId = childId;
		this.title = title;
		this.description = description;
		this.rewardPoints = rewardPoints;
		this.repeatType = repeatType;
		this.startDate = startDate != null ? startDate : LocalDate.now();
		this.endDate = endDate;
		this.isActive = true;
	}

	/**
	 * 오늘 이 집안일을 수행할 수 있는 기간인지 본다.
	 *
	 * <p>완료 요청(CHORE-11)에서 쓴다. 승인·거절은 이 검사를 하지 않는다 —
	 * 기간이 지났어도 이미 들어온 요청은 부모가 처리할 수 있어야 한다.
	 */
	public boolean isInPeriod(LocalDate date) {
		if (date.isBefore(startDate)) {
			return false;
		}
		return endDate == null || !date.isAfter(endDate);
	}

	/**
	 * 내용·보상·주기를 고친다 (CHORE-05). null 로 온 값은 그대로 둔다 — PATCH 라서다.
	 *
	 * <p>description 과 endDate 는 "비우기"와 "안 바꿈"을 null 하나로 구분할 수 없다.
	 * 지금은 둘 다 "안 바꿈"으로 본다. 비우는 기능이 필요해지면 그때 별도 표시를 받는다.
	 *
	 * <p>childId 는 여기 없다. 담당을 옮기면 이미 쌓인 완료 이력이 누구 것인지 모호해진다.
	 */
	public void update(String title, String description, Integer rewardPoints, ChoreRepeatType repeatType,
			LocalDate startDate, LocalDate endDate) {
		if (title != null) {
			this.title = title;
		}
		if (description != null) {
			this.description = description;
		}
		if (rewardPoints != null) {
			this.rewardPoints = rewardPoints;
		}
		if (repeatType != null) {
			this.repeatType = repeatType;
		}
		if (startDate != null) {
			this.startDate = startDate;
		}
		if (endDate != null) {
			this.endDate = endDate;
		}

		// 한쪽만 바꾸면 기간이 뒤집힐 수 있다. 요청 DTO 는 상대편의 현재 값을 모르므로
		// 합쳐진 최종 상태를 아는 여기서 본다.
		if (this.endDate != null && this.endDate.isBefore(this.startDate)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료일은 시작일과 같거나 이후여야 합니다");
		}
	}

	/**
	 * 목록에서 감춘다 (CHORE-06). 행을 지우지 않는 이유는 완료 이력이 chore_id 를 참조하기 때문이다.
	 * 진짜로 지우면 이미 지급한 보상의 근거가 사라진다.
	 *
	 * <p>비활성이어도 상세 조회와 대기 중인 완료 요청 처리는 계속 된다.
	 */
	public void deactivate() {
		this.isActive = false;
	}
}
