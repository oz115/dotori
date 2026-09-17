import { notificationApi } from '@/features/notification/api/notificationApi';
import {
  getFcmToken,
  getPushPermissionStatus,
  requestPushPermission,
} from '@/features/notification/lib/pushToken';

/**
 * 푸시 알림 권한을 확인하고, 허용돼 있으면 이 기기의 FCM 토큰을 서버에 등록한다.
 *
 * @param promptIfDenied 이전에 거부한 적이 있어도 OS 권한 다이얼로그를 다시 띄울지.
 *   로그인/앱 실행마다 자동으로 부르는 경우(false)는 한 번 거부한 사용자에게 매번 묻지
 *   않기 위해 거부 상태는 그대로 둔다. 마이페이지에서 사용자가 직접 "켜기"를 누른 경우(true)는
 *   명시적 의도가 있으니 다시 물어봐도 된다.
 *
 * 실패(권한 거부, 네트워크 오류, iOS 등)는 전부 조용히 무시한다 — 로그인/앱 실행을
 * 막을 이유가 없는 백그라운드 부가 작업이다.
 */
export async function ensurePushRegistered(promptIfDenied: boolean): Promise<boolean> {
  try {
    let status = await getPushPermissionStatus();
    if (status === 'undetermined' || (status === 'denied' && promptIfDenied)) {
      status = await requestPushPermission();
    }
    if (status !== 'granted') return false;

    const fcmToken = await getFcmToken();
    if (!fcmToken) return false;

    await notificationApi.registerDeviceToken({ fcmToken, platform: 'ANDROID' });
    return true;
  } catch {
    return false;
  }
}
