import { create } from 'zustand';

/**
 * 이체 퍼널이 화면(은행 선택 → 계좌번호·금액 → 확인)에 걸쳐 모으는 값.
 * bankKey 는 표시 전용 — 서버(TransferBody)에는 toAccountNo 만 보낸다.
 * 진입 시 `reset()`, 단계마다 `patch()`.
 */
type TransferState = {
  /** lib/banks 의 Bank.key */
  bankKey: string | null;
  /** '-' 없이 숫자만 */
  toAccountNo: string;
  /** 원 단위 */
  amount: number;
  memo: string;
  patch: (v: Partial<Omit<TransferState, 'patch' | 'reset'>>) => void;
  reset: () => void;
};

const initial = {
  bankKey: null as string | null,
  toAccountNo: '',
  amount: 0,
  memo: '',
};

export const useTransferStore = create<TransferState>((set) => ({
  ...initial,
  patch: (v) => set(v),
  reset: () => set(initial),
}));
