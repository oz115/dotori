package com.bana.dotori.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

/**
 * 하루 시작 알림.
 *
 * <p>퀴즈와 뉴스는 매일 새로 생기는데(00:05 뉴스, 00:10 퀴즈) 아이가 앱을 열기 전에는
 * 생긴 줄 모른다. 둘을 따로 알리면 아침마다 알림이 두 개씩 쌓이므로 "오늘 치가 준비됐다"
 * 한 번으로 묶는다.
 *
 * <p>가족에 속한 아이만 받는다. 퀴즈·뉴스가 모두 가족 경로 아래 있어서, 가족이 없으면
 * 알려 봐야 볼 것이 없다.
 *
 * <p>refId 는 없다. 퀴즈와 뉴스 중 어느 쪽을 가리켜야 할지 정할 수 없어, 앱이 홈으로
 * 보내도록 비워 둔다.
 */
@Service
@RequiredArgsConstructor
public class DailyBriefingService {

	private static final String TITLE = "새로운 하루가 시작됐어요";
	private static final String BODY = "오늘의 퀴즈와 소식이 준비됐어요";

	private final FamilyMemberRepository familyMemberRepository;
	private final PushNotificationService pushNotificationService;

	/**
	 * 아이 전원에게 하루 시작을 알린다.
	 *
	 * <p>읽기 전용이다. 알림함 기록은 발송 쪽이 자기 트랜잭션(REQUIRES_NEW)에서 남기므로,
	 * 한 명이 실패해도 나머지에게 이미 나간 알림이 되돌아가지 않는다.
	 *
	 * @return 보낸 건수
	 */
	@Transactional(readOnly = true)
	public int notifyChildren() {
		int sent = 0;
		for (FamilyMember child : familyMemberRepository.findAllActiveByRole(UserRole.CHILD)) {
			pushNotificationService.notify(child.getUser().getId(), NotificationType.DAILY, TITLE, BODY, null);
			sent++;
		}
		return sent;
	}
}
