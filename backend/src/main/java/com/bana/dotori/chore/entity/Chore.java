package com.bana.dotori.chore.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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
 * <p>삭제는 물리 삭제가 아니라 deletedAt 기록이다. 완료 이력이 choreId 를 참조하기 때문에
 * 행을 지우면 이미 지급한 보상의 근거가 사라진다.
 */
@Entity
@Table(name = "chores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chore {

	/**
	 * 시작일 기본값이 보는 "오늘"을 KST 로 고정한다.
	 *
	 * <p>서비스가 보통 시작일을 채워 주므로 이 기본값은 안전망이다. 다만 안전망이
	 * 환경에 따라 다른 날짜를 보면 안전망이 아니다 — {@code LocalDate.now()} 는
	 * 시스템 기본 타임존을 쓰고, 그 값은 배포 설정에 달려 있다
	 * (자세한 이유는 {@code ChoreServiceImpl.KST} 주석 참고).
	 */
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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

	/**
	 * AI 가 제목·설명을 보고 판정한 난이도. 보상 산정의 근거로 남긴다.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "difficulty", nullable = false)
	private ChoreDifficulty difficulty;

	/**
	 * AI 서버가 비동기로 산정하는 가치 가중치 (주간 기본 용돈 B의 배수).
	 * 등록 직후에는 산정 전이므로 null 이다.
	 */
	@Column(name = "value", precision = 10, scale = 2)
	private BigDecimal value;

	/**
	 * 승인 시점에 지급할 금액. {@code value} 산정이 끝나야 계산할 수 있어
	 * null 을 허용한다 (null = 보상 산정 중).
	 */
	@Column(name = "reward_points")
	private Integer rewardPoints;

	@Enumerated(EnumType.STRING)
	@Column(name = "repeat_type", nullable = false)
	private ChoreRepeatType repeatType;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	/** 없으면 기한 없이 계속 반복한다. */
	@Column(name = "end_date")
	private LocalDate endDate;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	/** 소프트 삭제 시각. 목록에서는 빠지지만 상세와 완료 요청 처리는 계속 된다. */
	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	/**
	 * startDate 가 없으면 오늘로 둔다.
	 *
	 * <p>DB 에도 DEFAULT CURRENT_DATE 가 걸려 있지만 Hibernate 는 INSERT 에 모든 컬럼을 실으므로
	 * null 을 그대로 두면 DEFAULT 가 아니라 NOT NULL 위반이 된다.
	 */
	@Builder
	private Chore(Integer familyId, Integer parentId, Integer childId, String title, String description,
			ChoreDifficulty difficulty, Integer rewardPoints, ChoreRepeatType repeatType,
			LocalDate startDate, LocalDate endDate) {
		this.familyId = familyId;
		this.parentId = parentId;
		this.childId = childId;
		this.title = title;
		this.description = description;
		this.difficulty = difficulty;
		this.rewardPoints = rewardPoints;
		this.repeatType = repeatType;
		this.startDate = startDate != null ? startDate : LocalDate.now(KST);
		this.endDate = endDate;
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
	 * 지금 주기가 시작된 순간 (KST 자정).
	 *
	 * <p>"이번 주기에 이미 보상을 받았나" 를 판정할 때 하한으로 쓴다. 크론이 주기가 끝날 때마다
	 * 기간을 앞으로 밀어 주므로, 이 시각 이후에 승인된 것은 이번 주기의 것뿐이다.
	 *
	 * <p>startDate 는 날짜라 시각이 없다. 어느 타임존의 자정인지 정하지 않으면 서버 설정에 따라
	 * 경계가 아홉 시간씩 밀리고, 한국 시간 자정 직후에 승인한 건이 지난 주기로 분류된다.
	 */
	public OffsetDateTime currentPeriodStartedAt() {
		return startDate.atStartOfDay(KST).toOffsetDateTime();
	}

	/**
	 * 이번 주기 보상 지급의 멱등키.
	 *
	 * <p>주기당 1회 지급을 실제로 보장하는 장치다. {@code point_transactions} 에
	 * {@code UNIQUE (child_id, idempotency_key)} 가 걸려 있어, 같은 주기에 두 번째 지급이
	 * 들어오면 DB 가 거부한다. 부모가 두 기기에서 동시에 승인해도 한 건만 남는다.
	 *
	 * <p>키에 startDate 를 넣는 이유는 크론이 기간을 밀면 키도 같이 바뀌어야 다음 주기에
	 * 다시 받을 수 있기 때문이다. 일회성(ONCE)은 기간이 밀리지 않으므로 영원히 한 번이다.
	 *
	 * <p>{@link #currentPeriodStartedAt()} 와 같은 startDate 에서 나온다. 사전 검사와 최종
	 * 방어가 같은 값을 보게 묶어 둔 것이다 — 따로 계산하면 둘이 어긋나는 날이 온다.
	 */
	public String rewardIdempotencyKey() {
		return "chore:" + id + ":" + startDate;
	}

	/**
	 * 내용·보상·주기를 고친다 (CHORE-05). null 로 온 값은 그대로 둔다 — PATCH 라서다.
	 *
	 * <p>description 과 endDate 는 "비우기"와 "안 바꿈"을 null 하나로 구분할 수 없다.
	 * 지금은 둘 다 "안 바꿈"으로 본다. 비우는 기능이 필요해지면 그때 별도 표시를 받는다.
	 *
	 * <p>childId 는 여기 없다. 담당을 옮기면 이미 쌓인 완료 이력이 누구 것인지 모호해진다.
	 */
	public void update(String title, String description, ChoreDifficulty difficulty, Integer rewardPoints) {
		if (title != null) {
			this.title = title;
		}
		if (description != null) {
			this.description = description;
		}
		if (difficulty != null) {
			this.difficulty = difficulty;
		}
		if (rewardPoints != null) {
			this.rewardPoints = rewardPoints;
		}
	}

	/**
	 * 주기와 기간을 함께 바꾼다 (CHORE-05).
	 *
	 * <p>셋을 따로 바꿀 수 없게 묶은 이유는, 주기만 바꾸면 기간이 예전 주기의 값으로
	 * 남기 때문이다. 일회성(9/10~9/20)을 매일로 바꾸면 "매일 반복인데 9/20 에 끝나는"
	 * 상태가 되고, 크론은 end_date &lt; today 만 밀기 때문에 9/21 이 돼야 정상으로 돌아온다.
	 *
	 * <p>어떤 기간을 넣을지는 서비스가 정한다 — 반복으로 바꾸면 오늘 기준으로 계산하고,
	 * 일회성으로 바꾸면 부모가 보낸 날짜를 쓴다.
	 */
	public void changePeriod(ChoreRepeatType repeatType, LocalDate startDate, LocalDate endDate) {
		if (endDate != null && endDate.isBefore(startDate)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료일은 시작일과 같거나 이후여야 합니다");
		}
		this.repeatType = repeatType;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	/**
	 * 소프트 삭제 (CHORE-06). 행을 지우지 않는 이유는 완료 이력이 chore_id 를 참조하기 때문이다.
	 * 진짜로 지우면 이미 지급한 보상의 근거가 사라진다.
	 *
	 * <p>지워도 상세 조회와 대기 중인 완료 요청 처리는 계속 된다.
	 * 이미 지운 것을 다시 지워도 시각만 갱신되고 실패하지 않는다.
	 */
	public void softDelete() {
		this.deletedAt = OffsetDateTime.now();
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	/**
	 * AI 산정 결과를 반영한다. 가중치와 그것으로 계산한 보상을 함께 받는다.
	 *
	 * <p>둘을 따로 넣을 수 있게 두면 value 와 reward_points 가 어긋난 행이 생긴다.
	 */
	public void applyReward(BigDecimal value, Integer rewardPoints) {
		this.value = value;
		this.rewardPoints = rewardPoints;
	}
}
