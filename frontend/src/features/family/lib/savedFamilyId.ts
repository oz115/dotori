import * as SecureStore from 'expo-secure-store';

/**
 * 방금 만든/속한 가족의 id 를 기억한다.
 *
 * 서버가 "내 가족 id" 를 알려주는 API 가 아직 없어서 (GET /families/status 는
 * boolean 만 준다), 가족 삭제·초대 코드 재발급처럼 familyId 가 필요한 화면을 위해
 * 생성/연동 성공 시점에 저장해 둔다. 서버에 해당 API 가 생기면 이 파일은 없앤다.
 */
const KEY = 'dotori.family.id';

export const savedFamilyId = {
  async get(): Promise<number | null> {
    const raw = await SecureStore.getItemAsync(KEY);
    const n = raw == null ? NaN : Number(raw);
    return Number.isInteger(n) ? n : null;
  },
  set(familyId: number): Promise<void> {
    return SecureStore.setItemAsync(KEY, String(familyId));
  },
  clear(): Promise<void> {
    return SecureStore.deleteItemAsync(KEY);
  },
};
