package com.bana.dotori.chore.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
 * 아이가 올린 집안일 완료 인증 (CHORE-03·09·10·11).
 *
 * <p>지급 금액은 여기 남기지 않는다. 승인 시점의 {@code chores.reward_points} 를 지급하는 것으로
 * 통일했기 때문이다 — 요청 시점 값을 따로 스냅샷해 두면 부모가 금액을 고친 뒤 승인했을 때
 * 어느 쪽을 줘야 하는지가 두 갈래로 갈린다.
 *
 * <p><b>주기당 1회 지급</b>은 두 겹으로 막는다.
 * <ol>
 *   <li>사전 검사 — 이번 주기에 승인된 이력이 있는지 보고 409 로 돌려보낸다.
 *       사용자에게 이유를 알려 주는 것이 목적이다
 *       ({@code ChoreCompletionRequestRepository#existsApprovedIn}).</li>
 *   <li>최종 방어 — 지급 멱등키를 {@code chore:{choreId}:{startDate}} 로 만들어
 *       {@code point_transactions} 의 {@code UNIQUE (child_id, idempotency_key)} 에 맡긴다.</li>
 * </ol>
 * 1번만 두면 부모가 두 기기에서 동시에 승인했을 때 둘 다 검사를 통과해 이중 지급된다.
 * 검사와 INSERT 사이에 틈이 있기 때문이고, 그 틈은 DB 제약으로만 닫을 수 있다.
 */
@Entity
@Table(name = "chore_completion_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChoreCompletionRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chore_completion_request_id")
	private Integer id;

	@Column(name = "chore_id", nullable = false, updatable = false)
	private Integer choreId;

	/**
	 * 요청을 올린 아이. {@code chores.child_id} 와 같은 값이지만 여기에도 둔다 —
	 * 아이별 완료 내역(CHORE-12)을 볼 때마다 chores 를 조인하지 않으려는 것이다.
	 */
	@Column(name = "child_id", nullable = false, updatable = false)
	private Integer childId;

	/** 인증 사진. 업로드 API(CHORE-13)가 아직 없어 지금은 항상 null 이다. */
	@Column(name = "photo_url", length = 100)
	private String photoUrl;

	@Column(name = "description", length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ChoreCompletionStatus status;

	@CreationTimestamp
	@Column(name = "requested_at", nullable = false, updatable = false)
	private OffsetDateTime requestedAt;

	/** 승인·거절·취소된 시각. 대기 중에는 null 이다. */
	@Column(name = "processed_at")
	private OffsetDateTime processedAt;

	@Builder
	private ChoreCompletionRequest(Integer choreId, Integer childId, String photoUrl, String description) {
		this.choreId = choreId;
		this.childId = childId;
		this.photoUrl = photoUrl;
		this.description = description;
		this.status = ChoreCompletionStatus.PENDING;
	}

	/** 부모가 승인한다 (CHORE-09). 보상 지급은 서비스가 별도로 처리한다. */
	public void approve(OffsetDateTime at) {
		transitionTo(ChoreCompletionStatus.SUCCEED, at);
	}

	/**
	 * 부모가 거절한다 (CHORE-10).
	 *
	 * <p>거절당한 아이는 같은 주기에 다시 요청할 수 있다. 막는 것은 <b>지급</b>이지 요청이 아니다 —
	 * 재도전을 막으면 부모가 잘못 눌렀을 때 되돌릴 방법이 없다.
	 */
	public void reject(OffsetDateTime at) {
		transitionTo(ChoreCompletionStatus.REJECTED, at);
	}

	/** 아이가 스스로 물린다 (CHORE-11). */
	public void cancel(OffsetDateTime at) {
		transitionTo(ChoreCompletionStatus.CANCELLED, at);
	}

	public boolean isPending() {
		return status.isPending();
	}

	/**
	 * 대기 중일 때만 상태를 옮긴다.
	 *
	 * <p>이 검사가 없으면 부모가 승인 버튼을 두 번 눌렀을 때 두 번째 호출도 통과해
	 * 보상이 두 번 나간다. 지급 멱등키가 최종적으로는 막아 주지만, 거기까지 가기 전에
	 * 여기서 걸러야 "이미 처리됨" 이라는 제대로 된 응답이 나간다.
	 */
	private void transitionTo(ChoreCompletionStatus next, OffsetDateTime at) {
		if (!isPending()) {
			throw new BusinessException(ErrorCode.CHORE_REQUEST_NOT_PENDING,
					"이미 처리된 요청입니다. requestId=" + id + ", status=" + status);
		}
		this.status = next;
		this.processedAt = at;
	}
}
