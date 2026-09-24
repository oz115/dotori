import { create } from 'zustand';

import type { FinancialKnowledgeLevel } from '@/features/family/model/types';

/**
 * 부모 온보딩 퍼널이 여러 화면(가족 이름 → 경제 규칙)에 걸쳐 모으는 값.
 * 가족 생성은 이 값들을 한 번의 POST /families 로 보낸다 (부분 저장 없음).
 * 온보딩 진입 시 `reset()`, "다음" 마다 `patch()`.
 *
 * 금액은 화면 단위인 원(₩)으로 들고 있다가 그대로 서버에 보낸다 (서버가 포인트로 환산).
 */
type OnboardingState = {
  familyName: string;
  weeklyAllowance: number | null;
  weeklyRewardLimit: number | null;
  financialKnowledgeLevel: FinancialKnowledgeLevel | null;

  patch: (values: Partial<Omit<OnboardingState, 'patch' | 'reset'>>) => void;
  reset: () => void;
};

const initial = {
  familyName: '',
  weeklyAllowance: null as number | null,
  weeklyRewardLimit: null as number | null,
  financialKnowledgeLevel: null as FinancialKnowledgeLevel | null,
};

export const useOnboardingStore = create<OnboardingState>((set) => ({
  ...initial,
  patch: (values) => set(values),
  reset: () => set(initial),
}));
