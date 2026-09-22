import { router, useFocusEffect, useLocalSearchParams, type Href } from 'expo-router';
import { useCallback } from 'react';
import { BackHandler } from 'react-native';

/** 알림함에서 들어온 화면임을 나타내는 라우트 파라미터 값. resolveNotificationRoute 가 실어 보낸다. */
export const FROM_NOTIFICATION = 'notification';

/**
 * 알림함에서 열린 화면의 뒤로가기를 알림함으로 돌린다.
 *
 * <p>알림함은 `(tabs)` 바깥에 있고, 알림이 가리키는 화면은 탭 안쪽에 있다. 탭 안쪽으로
 * 이동하면 expo-router 가 스택을 `(tabs)` 까지 되감아서 알림함 화면이 걷혀 버린다 —
 * 그래서 뒤로가기를 누르면 알림함이 아니라 그 탭의 목록(미션·상점)이 나왔다 (QA).
 *
 * <p>걷힌 화면을 되살릴 방법은 없으니 알림함을 다시 올린다. 헤더의 뒤로가기 버튼과 안드로이드
 * 하드웨어 뒤로가기를 둘 다 덮어야 한다 — 한쪽만 덮으면 같은 화면에서 두 버튼이 다른 곳으로 간다.
 *
 * @param notificationsHref 돌아갈 알림함 경로. `(child)`/`(parent)` 둘 다 있는 경로라
 *   그룹을 아는 라우트가 정확한 값을 넘긴다.
 * @returns onBack — 알림함에서 온 경우에만 함수, 아니면 undefined (화면이 원래 동작을 쓴다)
 */
export function useBackToNotifications(notificationsHref: Href): {
  fromNotification: boolean;
  onBack: (() => void) | undefined;
} {
  const { from } = useLocalSearchParams<{ from?: string }>();
  const fromNotification = from === FROM_NOTIFICATION;

  // push 로 다시 올린다. replace 는 지금 화면이 탭 내부 스택에 있어서, 탭 바깥 경로로
  // 갈아끼우면 어느 내비게이터가 바뀌는지가 분명하지 않다.
  const goToNotifications = useCallback(() => {
    router.push(notificationsHref);
  }, [notificationsHref]);

  useFocusEffect(
    useCallback(() => {
      if (!fromNotification) return;

      const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
        goToNotifications();
        return true; // 기본 뒤로가기(탭 목록으로 감)를 막는다
      });
      return () => subscription.remove();
    }, [fromNotification, goToNotifications]),
  );

  return { fromNotification, onBack: fromNotification ? goToNotifications : undefined };
}
