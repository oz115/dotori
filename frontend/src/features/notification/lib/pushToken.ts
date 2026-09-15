import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

/**
 * 네이티브 FCM 디바이스 토큰 발급. 백엔드가 Firebase Admin SDK로 직접 send() 하므로
 * Expo push 토큰이 아니라 OS 네이티브 토큰이 필요하다 — `getDevicePushTokenAsync()`가
 * Android에서 그 토큰을 그대로 준다.
 *
 * iOS는 FCM 연동(GoogleService-Info.plist)이 아직 없어 범위 밖이다 — 항상 null.
 */
export async function getFcmToken(): Promise<string | null> {
  if (Platform.OS !== 'android') return null;

  const { data } = await Notifications.getDevicePushTokenAsync();
  return data;
}

/** OS 권한 재요청 없이 지금 상태만 확인한다. */
export async function getPushPermissionStatus(): Promise<Notifications.PermissionStatus> {
  const { status } = await Notifications.getPermissionsAsync();
  return status;
}

/** OS 권한 다이얼로그를 띄운다(이미 허용/거부된 상태면 OS가 알아서 다이얼로그 없이 응답). */
export async function requestPushPermission(): Promise<Notifications.PermissionStatus> {
  const { status } = await Notifications.requestPermissionsAsync();
  return status;
}
