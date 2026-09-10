import type { FinancialKnowledgeLevel } from '@/features/family/model/types';

/**
 * 온보딩 입력 규칙과 선택지 데이터.
 * 금액 최소·단위는 서버(FamilyServiceImpl.ONBOARDING_AMOUNT_UNIT_WON)와 같은 값이어야 한다.
 */

/** 온보딩 금액의 최소값이자 입력 단위(원). */
export const AMOUNT_UNIT_WON = 1000;

/** 주간 기본 용돈 프리셋(원). 직접 입력도 가능. */
export const ALLOWANCE_PRESETS_WON = [3000, 5000, 10000, 20000] as const;

/** 주간 추가 보상 한도 프리셋(원). */
export const REWARD_LIMIT_PRESETS_WON = [2000, 5000, 10000, 30000] as const;

/** 최소 1,000원 이상 · 1,000원 단위인지. 서버 검증과 동일한 규칙을 미리 막는다. */
export function isValidOnboardingAmount(won: number | null): won is number {
  return (
    won != null && Number.isInteger(won) && won >= AMOUNT_UNIT_WON && won % AMOUNT_UNIT_WON === 0
  );
}

/** "50,000" 처럼 콤마가 섞인 입력 문자열을 숫자로. 숫자가 아니면 null. */
export function parseAmountInput(text: string): number | null {
  const digits = text.replace(/[^0-9]/g, '');
  if (digits === '') return null;
  return Number(digits);
}

export const KNOWLEDGE_LEVELS: {
  key: FinancialKnowledgeLevel;
  title: string;
  description: string;
}[] = [
  {
    key: 'BEGINNER',
    title: '처음이에요',
    description: '용돈과 저축의 개념부터 차근차근 배워요.',
  },
  {
    key: 'INTERMEDIATE',
    title: '알아가고 있어요',
    description: '예금·적금과 이자를 조금 알고 있어요.',
  },
  {
    key: 'ADVANCED',
    title: '익숙해요',
    description: '투자의 수익과 위험 관계를 이해하고 있어요.',
  },
];
