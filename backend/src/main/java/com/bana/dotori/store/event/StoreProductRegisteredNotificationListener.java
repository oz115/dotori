package com.bana.dotori.store.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;
import com.bana.dotori.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

/**
 * 상품 등록 트랜잭션이 커밋된 뒤에만, 그 가족 자녀들에게 알림을 보낸다 (S15P21A505-230).
 *
 * <p>AFTER_COMMIT 에서만 동작하므로, 등록 트랜잭션이 롤백되면 이 리스너 자체가 호출되지 않는다.
 * {@link StoreProductValueEvaluationListener} 와 같은 {@link StoreProductCreatedEvent} 를
 * 듣는 별개의 리스너다 — 하나는 AI 가치 산정 큐 발행, 하나는 알림 발송으로 책임이 다르다.
 */
@Component
@RequiredArgsConstructor
public class StoreProductRegisteredNotificationListener {

	private final FamilyMemberRepository familyMemberRepository;
	private final PushNotificationService pushNotificationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onProductCreated(StoreProductCreatedEvent event) {
		String title = "새 상품이 등록됐어요";
		String body = event.name() + "이(가) 상점에 새로 등록됐어요";

		for (FamilyMember child : familyMemberRepository.findAllChildrenByFamilyId(event.familyId(), UserRole.CHILD)) {
			pushNotificationService.notify(child.getUser().getId(), NotificationType.PURCHASE, title, body,
				event.storeProductId());
		}
	}
}
