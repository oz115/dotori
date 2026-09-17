package com.bana.dotori.chore.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;

import lombok.RequiredArgsConstructor;

/**
 * 완료 요청의 생애주기(요청 → 승인/거절)를 알린다 (S15P21A505-244·245).
 *
 * <p>세 이벤트를 한 클래스에 모았다. 상대방이 다음 행동을 하도록 떠미는 같은 흐름이라
 * 문구를 나란히 두고 맞추는 편이 읽기 쉽다.
 *
 * <p>refId 는 셋 다 choreId 다. 앱이 알림을 눌렀을 때 완료 요청이 아니라 할 일 상세로
 * 보내야 하기 때문이다 — 거절된 요청은 이미 사라지고 없다.
 */
@Component
@RequiredArgsConstructor
public class ChoreCompletionNotificationListener {

	private final PushNotificationService pushNotificationService;

	/** 아이가 완료를 인증하면 할 일을 등록한 부모에게 알린다 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onRequested(ChoreCompletionRequestedEvent event) {
		pushNotificationService.notify(event.parentId(), NotificationType.CHORE,
			"완료 요청이 왔어요", event.childName() + " · " + event.choreTitle(), event.choreId());
	}

	/** 승인은 지급이 끝난 뒤다. 실제로 들어간 금액을 그대로 알린다 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onApproved(ChoreCompletionApprovedEvent event) {
		pushNotificationService.notify(event.childId(), NotificationType.CHORE,
			"완료 승인됐어요", event.choreTitle() + " · +" + event.rewardPoints() + " 도토리",
			event.choreId());
	}

	/**
	 * 거절은 사유를 담지 않는다. 서버가 저장하지 않아 담을 값이 없다.
	 *
	 * <p>대신 끝이 아니라는 것이 드러나는 문구를 쓴다 — 거절돼도 같은 주기에 다시 요청할 수 있다.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onRejected(ChoreCompletionRejectedEvent event) {
		pushNotificationService.notify(event.childId(), NotificationType.CHORE,
			"다시 해볼까요?", event.choreTitle() + " 완료 요청이 아직 통과하지 못했어요",
			event.choreId());
	}
}
