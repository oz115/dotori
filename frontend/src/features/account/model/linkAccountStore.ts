import { create } from 'zustand';

/**
 * 계좌 연동 퍼널이 화면(은행 선택 → 계좌번호 → 1원 인증)에 걸쳐 모으는 값.
 * 진입 시 `reset()`, 단계마다 `patch()`.
 */
type LinkAccountState = {
  /** lib/banks 의 Bank.key */
  bankKey: string | null;
  /** '-' 없이 숫자만 */
  accountNo: string;
  patch: (v: Partial<Omit<LinkAccountState, 'patch' | 'reset'>>) => void;
  reset: () => void;
};

const initial = { bankKey: null as string | null, accountNo: '' };

export const useLinkAccountStore = create<LinkAccountState>((set) => ({
  ...initial,
  patch: (v) => set(v),
  reset: () => set(initial),
}));
