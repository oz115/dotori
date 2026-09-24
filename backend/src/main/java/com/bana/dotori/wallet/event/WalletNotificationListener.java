package com.bana.dotori.wallet.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 지갑에서 돈이 오가는 지점을 알린다 — 환전 요청·승인·거절, 충전, 지급.
 *
 * <p>다섯 갈래를 한 클래스에 모았다. 전부 "잔액이 움직였다" 는 같은 성격이라 금액 표기와
 * 문구를 나란히 두고 맞추는 편이 낫다.
 *
 * <p>아이 이름은 여기서 읽는다. 서비스는 식별자만 발행하고, 표시용 문구를 만드는 것은
 * 알림 쪽 관심사다 — 이름을 쓰려고 지갑 서비스가 사용자 저장소를 알게 만들 이유가 없다.
 */
@Component
@RequiredArgsConstructor
public class WalletNotificationListener {

	private final UserRepository userRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final PushNotificationService pushNotificationService;

	/** 환전 요청 → 부모. 승인해야 송금이 일어나므로 부모가 봐야 진행된다 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCashoutRequested(PointCashoutRequestedEvent event) {
		pushNotificationService.notify(event.parentId(), NotificationType.CASHOUT,
			"환전 요청이 왔어요",
			childName(event.childId()) + " · " + event.pointAmount() + " 도토리",
			event.cashoutRequestId());
	}

	/** 환전 승인 → 아이. 이 시점에 송금까지 끝나 있다 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCashoutApproved(PointCashoutApprovedEvent event) {
		pushNotificationService.notify(event.childId(), NotificationType.CASHOUT,
			"환전이 완료됐어요", event.pointAmount() + " 도토리를 보냈어요",
			event.cashoutRequestId());
	}

	/**
	 * 환전 거절 → 아이.
	 *
	 * <p>거절되면 요청할 때 빠져나갔던 도토리가 되돌아온다. 그 사실을 함께 알린다 — 알림이
	 * 없으면 아이는 도토리가 사라진 줄 안다.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCashoutDeclined(PointCashoutDeclinedEvent event) {
		pushNotificationService.notify(event.childId(), NotificationType.CASHOUT,
			"환전이 취소됐어요", event.pointAmount() + " 도토리가 다시 들어왔어요",
			event.cashoutRequestId());
	}

	/**
	 * 충전 → 부모 전원.
	 *
	 * <p>아이가 실제 돈을 부모 계좌로 보낸 거래다. 계좌에 입금이 생기는 일이라 부모가
	 * 모르고 지나가면 안 된다. refId 는 없다 — 충전은 들여다볼 상세 화면이 없다.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCharged(PointChargedEvent event) {
		String body = childName(event.childId()) + "이(가) " + event.cashAmount() + "원을 충전했어요";
		for (FamilyMember parent : familyMemberRepository.findAllByFamilyIdAndRole(event.familyId(),
			UserRole.PARENT)) {
			pushNotificationService.notify(parent.getUser().getId(), NotificationType.CHARGE,
				"아이가 충전했어요", body, null);
		}
	}

	/** 지급 → 아이. 부모가 넣어 준 도토리는 아이가 알아야 쓴다 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onGranted(PointGrantedEvent event) {
		pushNotificationService.notify(event.childId(), NotificationType.CHARGE,
			"도토리를 받았어요", "부모님이 " + event.amount() + " 도토리를 주셨어요", null);
	}

	/**
	 * 알림 문구에 쓸 아이 이름. 못 찾으면 "아이" 로 둔다.
	 *
	 * <p>여기서 예외를 던지면 이미 끝난 거래가 알림 때문에 실패한 것처럼 보인다. 이름은
	 * 거래의 일부가 아니라 문구의 일부다.
	 */
	private String childName(Integer childId) {
		return userRepository.findById(childId).map(user -> user.getName()).orElse("아이");
	}
}
