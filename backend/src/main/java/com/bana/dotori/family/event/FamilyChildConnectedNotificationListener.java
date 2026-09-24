package com.bana.dotori.family.event;

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
 * 아이가 가족에 합류하면 양쪽에 알린다.
 *
 * <p>부모에게는 "누가 들어왔는지", 아이에게는 "어느 가족에 연결됐는지" 를 알린다. 서로
 * 궁금한 것이 달라서 문구를 나눈다.
 *
 * <p>부모 전원에게 보낸다. 가족을 만든 부모만 알면 되는 일이 아니고, 코드를 건네준
 * 사람과 앱을 보는 사람이 다를 수 있다.
 *
 * <p>refId 는 familyId 다. 알림을 눌렀을 때 보낼 만한 곳이 가족 화면뿐이다.
 */
@Component
@RequiredArgsConstructor
public class FamilyChildConnectedNotificationListener {

	private final FamilyMemberRepository familyMemberRepository;
	private final PushNotificationService pushNotificationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onChildConnected(FamilyChildConnectedEvent event) {
		for (FamilyMember parent : familyMemberRepository.findAllByFamilyIdAndRole(event.familyId(),
			UserRole.PARENT)) {
			pushNotificationService.notify(parent.getUser().getId(), NotificationType.INVITATION,
				"아이가 가족에 들어왔어요", event.childName() + "이(가) 연결됐어요", event.familyId());
		}

		pushNotificationService.notify(event.childId(), NotificationType.INVITATION,
			"가족에 연결됐어요", event.familyName() + " 가족이 되었어요", event.familyId());
	}
}
