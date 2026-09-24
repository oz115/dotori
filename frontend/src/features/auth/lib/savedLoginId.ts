import * as SecureStore from 'expo-secure-store';

/**
 * "아이디 저장" 체크 시 로그인 아이디를 기억한다. (비밀값은 아니지만 앱이 이미
 * secure-store 에 의존하므로 별도 저장소를 추가하지 않고 여기 둔다.)
 */
const SAVED_LOGIN_ID_KEY = 'dotori.auth.savedLoginId';

export const savedLoginId = {
  get(): Promise<string | null> {
    return SecureStore.getItemAsync(SAVED_LOGIN_ID_KEY);
  },
  set(loginId: string): Promise<void> {
    return SecureStore.setItemAsync(SAVED_LOGIN_ID_KEY, loginId);
  },
  clear(): Promise<void> {
    return SecureStore.deleteItemAsync(SAVED_LOGIN_ID_KEY);
  },
};
