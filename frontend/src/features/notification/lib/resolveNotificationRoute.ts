import type { Href } from 'expo-router';

type Role = 'PARENT' | 'CHILD' | undefined;

/**
 * 알림 탭으로 받은 FCM data 페이로드(백엔드 PushNotificationServiceImpl 이 실어 보낸
 * type/refId)를 실제 라우트로 바꾼다. FCM data 값은 항상 string 이라 unknown 에서 직접
 * 좁혀 쓴다. 처리할 수 없는 조합(모르는 타입, refId 없음, role 미확인)은 null 을 반환하고,
 * 호출자는 아무 것도 하지 않는다(현재 OS 기본 동작에 맡김).
 */
export function resolveNotificationRoute(
  data: Record<string, unknown> | undefined,
  role: Role,
): Href | null {
  const type = data?.type;
  const refId = data?.refId;
  if (typeof type !== 'string' || typeof refId !== 'string' || !role) {
    return null;
  }

  switch (type) {
    case 'PURCHASE':
      if (role === 'PARENT') {
        return { pathname: '/(parent)/(tabs)/store/[productId]', params: { productId: refId } };
      }
      return { pathname: '/(child)/(tabs)/store/[productId]', params: { productId: refId } };
    default:
      return null;
  }
}
