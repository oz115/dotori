import * as SecureStore from 'expo-secure-store';

/**
 * refresh token 전용 보안 저장소.
 *
 * iOS Keychain / Android Keystore(암호화)에 저장한다. 일반 저장소(AsyncStorage)와 달리
 * 다른 앱이나 기기 백업에서 값을 꺼내볼 수 없다. access token 은 짧게 살고 메모리에만
 * 두므로 여기 저장하지 않는다.
 */
const REFRESH_TOKEN_KEY = 'dotori.auth.refreshToken';

export const tokenStorage = {
  get(): Promise<string | null> {
    return SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
  },
  set(token: string): Promise<void> {
    return SecureStore.setItemAsync(REFRESH_TOKEN_KEY, token, {
      // 기기 잠금 해제 상태에서만 접근, iCloud 등에 동기화하지 않음
      keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
    });
  },
  clear(): Promise<void> {
    return SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
  },
};
