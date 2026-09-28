import * as SecureStore from 'expo-secure-store';
import { create } from 'zustand';

/**
 * "한 번이라도 저금 목표로 떠 있던 상품" 기억 (아이별). 잔액이 목표가를 넘은 뒤에도
 * 홈 카드에 "이제 살 수 있어요"로 남기려면 과거에 떠 있었는지를 알아야 하는데,
 * 서버에는 그런 이력이 없어 기기에 기억한다. 금융정보가 아니라 상품 id 목록뿐이고,
 * 아이별(childId)로 갈라 저장해 계정을 바꿔도 남의 목표가 섞이지 않는다.
 *
 * SecureStore 는 비동기라 앱을 켠 직후엔 비어 있다 — 위젯이 `hydrate()`를 기다렸다가
 * 그린다 (기다리지 않으면 달성 카드가 사라졌다 나타나며 깜빡인다).
 */

const STORAGE_KEY = 'store.goal.seen.v1';

type GoalMemoryState = {
  /** childId(문자열) -> 목표로 떠 있던 상품 id 목록 */
  seenByChild: Record<string, number[]>;
  hydrated: boolean;
  hydrate: () => Promise<void>;
  markSeen: (childId: number, productIds: number[]) => void;
};

export const useGoalMemoryStore = create<GoalMemoryState>((set, get) => ({
  seenByChild: {},
  hydrated: false,

  hydrate: async () => {
    if (get().hydrated) return;
    try {
      const raw = await SecureStore.getItemAsync(STORAGE_KEY);
      set({ seenByChild: raw ? JSON.parse(raw) : {}, hydrated: true });
    } catch {
      // 저장소가 깨져 있어도 기능은 살린다 — 기억이 없으면 목표만 다시 떠서 다시 쌓인다
      set({ seenByChild: {}, hydrated: true });
    }
  },

  markSeen: (childId, productIds) => {
    const key = String(childId);
    const prev = get().seenByChild[key] ?? [];
    const merged = [...new Set([...prev, ...productIds])];
    if (merged.length === prev.length) {
      return;
    }
    const seenByChild = { ...get().seenByChild, [key]: merged };
    set({ seenByChild });
    void SecureStore.setItemAsync(STORAGE_KEY, JSON.stringify(seenByChild)).catch(() => {
      // 저장 실패는 치명적이지 않다 — 다음 앱 실행에서 달성 카드가 안 보일 뿐이다
    });
  },
}));
