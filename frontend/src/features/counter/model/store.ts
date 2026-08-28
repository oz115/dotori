import { create } from 'zustand';

type CounterState = {
  count: number;
  increment: () => void;
  decrement: () => void;
  reset: () => void;
};

/**
 * features 레이어 예시.
 * FSD-lite에서는 entities 레이어를 따로 두지 않으므로, 이 feature가
 * 다루는 상태(count)와 그 상태를 변경하는 동작(increment 등)을 함께 소유합니다.
 */
export const useCounterStore = create<CounterState>((set) => ({
  count: 0,
  increment: () => set((state) => ({ count: state.count + 1 })),
  decrement: () => set((state) => ({ count: state.count - 1 })),
  reset: () => set({ count: 0 }),
}));
