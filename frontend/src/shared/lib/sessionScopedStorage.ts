import * as SecureStore from 'expo-secure-store';

/**
 * "로그인한 사람"에 묶인 SecureStore 키들.
 *
 * 기기 저장소는 계정과 무관하게 남기 때문에, 로그아웃 후 다른 계정으로 들어오면
 * 이전 사용자의 값이 그대로 읽힌다(예: 남의 가족 id 로 지갑을 조회). 그래서 세션이
 * 끝날 때 함께 지워야 하는 키는 여기 모아 두고 auth 의 로그아웃/탈퇴가 한 번에 비운다.
 *
 * 로그인 아이디(savedLoginId)처럼 "기기에 남겨도 되는" 값은 여기 넣지 않는다.
 */
export const sessionScopedKeys = {
  /**
   * 예전에 가족 id 를 기기에 저장하던 키. 지금은 서버(`GET /families/status`)가 알려주므로
   * 쓰지 않지만, 이미 설치된 기기에 남아 있는 값을 정리하려고 목록에 남겨 둔다.
   */
  legacyFamilyId: 'dotori.family.id',
} as const;

/** 로그아웃·회원 탈퇴 시 세션에 묶인 로컬 저장값을 모두 비운다. */
export async function clearSessionScopedStorage(): Promise<void> {
  await Promise.all(
    Object.values(sessionScopedKeys).map((key) => SecureStore.deleteItemAsync(key)),
  );
}
