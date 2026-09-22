package com.bana.dotori.chore.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bana.dotori.global.util.NotificationText;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;
import com.bana.dotori.user.repository.UserRepository;

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

	private final UserRepository userRepository;
	private final PushNotificationService pushNotificationService;

	/** 아이가 완료를 인증하면 할 일을 등록한 부모에게 알린다 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onRequested(ChoreCompletionRequestedEvent event) {
		pushNotificationService.notify(event.parentId(), NotificationType.CHORE,
			"완료 요청이 왔어요", childName(event.childId()) + " · " + event.choreTitle(), event.choreId());
	}

	/**
	 * 승인은 지급이 끝난 뒤다. 실제로 들어간 금액을 그대로 알린다.
	 *
	 * <p>아이가 읽는 알림이라 화면과 같은 말("미션")을 쓰고, 기호(`· +`) 대신 문장으로 적는다 —
	 * 초등학생에게 "제목 · +100 도토리" 는 무엇이 일어났는지 말해 주지 않는다 (QA).
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onApproved(ChoreCompletionApprovedEvent event) {
		pushNotificationService.notify(event.childId(), NotificationType.CHORE,
			"완료 승인이 되었어요.",
			NotificationText.shortName(event.choreTitle()) + "라는 미션을 완료했어요. "
				+ event.rewardPoints() + " 도토리를 얻었어요.",
			event.choreId());
	}

	/**
	 * 거절은 사유를 담지 않는다. 서버가 저장하지 않아 담을 값이 없다.
	 *
	 * <p>대신 끝이 아니라는 것과 무엇을 하면 되는지를 적는다 — 거절돼도 같은 주기에 다시
	 * 요청할 수 있다. 전에 쓰던 "아직 통과하지 못했어요" 는 시험에 떨어졌다는 말처럼 읽히고,
	 * 읽고 나서 무엇을 해야 하는지도 알 수 없었다 (QA). 아이 상세 화면의 안내와 같은 말을 쓴다.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onRejected(ChoreCompletionRejectedEvent event) {
		pushNotificationService.notify(event.childId(), NotificationType.CHORE,
			"다시 해볼까요?", event.choreTitle() + " · 사진을 다시 찍어서 보내주세요",
			event.choreId());
	}

	/**
	 * 알림 문구에 쓸 아이 이름. 못 찾으면 "아이" 로 둔다.
	 *
	 * 이름은 거래의 일부가 아니라 문구의 일부다 - 여기서 예외를 던지면 이미 끝난 요청이
	 * 알림 때문에 실패한 것처럼 보인다.
	 */
	private String childName(Integer childId) {
		return userRepository.findById(childId).map(user -> user.getName()).orElse("아이");
	}
}
