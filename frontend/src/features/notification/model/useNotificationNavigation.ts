import * as Notifications from 'expo-notifications';
import { router } from 'expo-router';
import { useEffect } from 'react';

import { resolveNotificationRoute } from '@/features/notification/lib/resolveNotificationRoute';

/**
 * 알림을 탭해서 앱이 열렸을 때(콜드 스타트 포함) 그 알림이 가리키는 화면으로 이동한다.
 * `useLastNotificationResponse`는 앱이 완전히 꺼진 상태에서 탭한 경우와 실행 중에 탭한
 * 경우를 둘 다 같은 값으로 알려주는 expo-notifications 공식 훅이다.
 *
 * role 은 features 끼리 직접 import 하지 않는 규칙 때문에 호출자(app/_layout.tsx)가
 * useAuthStore 에서 읽어 넘겨준다.
 */
export function useNotificationNavigation(role: 'PARENT' | 'CHILD' | undefined) {
  const response = Notifications.useLastNotificationResponse();

  useEffect(() => {
    if (!response) return;

    const route = resolveNotificationRoute(response.notification.request.content.data, role);
    if (route) {
      // push는 (tabs)가 이미 스택에 있어도 항상 새 인스턴스를 하나 더 쌓는다 — 그러면 탭
      // 인스턴스가 두 개로 쪼개져서 뒤로가기·탭 전환이 다 꼬인다. navigate는 기존 (tabs)
      // 인스턴스를 재사용해 포커스만 그 탭으로 옮긴다.
      router.navigate(route);
    }
  }, [response, role]);
}
