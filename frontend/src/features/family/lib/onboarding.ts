import type { FinancialKnowledgeLevel } from '@/features/family/model/types';
import { formatWon } from '@/shared/lib/money';

/**
 * 온보딩 입력 규칙과 선택지 데이터.
 * 금액 최소·단위는 서버(FamilyServiceImpl.ONBOARDING_AMOUNT_UNIT_WON)와 같은 값이어야 한다.
 */

/** 온보딩 금액의 최소값이자 입력 단위(원). */
export const AMOUNT_UNIT_WON = 1000;

/**
 * 금액 입력 상한(원). 서버에는 상한 검증이 없어서 키보드로 0 을 계속 붙이면 한도 계산·저장 범위를
 * 넘는 값이 들어갈 수 있다. 초등학생 주간 용돈·보상으로는 넉넉한 값으로 화면에서 막는다.
 */
export const MAX_AMOUNT_WON = 1_000_000;

/** 금액 입력 화면 공통 안내 문구 */
export const AMOUNT_HINT = `${formatWon(AMOUNT_UNIT_WON)} 이상, ${formatWon(AMOUNT_UNIT_WON)} 단위로 입력해요. (최대 ${formatWon(MAX_AMOUNT_WON)})`;

/** 금액 빠른 추가 버튼(원). 누른 만큼 현재 금액에 더해진다. */
export const AMOUNT_ADD_STEPS_WON = [1000, 5000, 10000] as const;

/** 1,000원 이상 · 1,000원 단위 · 상한 이하인지. 서버 검증과 같은 규칙을 미리 막는다. */
export function isValidOnboardingAmount(won: number | null): won is number {
  return (
    won != null &&
    Number.isInteger(won) &&
    won >= AMOUNT_UNIT_WON &&
    won <= MAX_AMOUNT_WON &&
    won % AMOUNT_UNIT_WON === 0
  );
}

/** "50,000" 처럼 콤마가 섞인 입력 문자열을 숫자로. 숫자가 아니면 null. 상한은 넘지 않게 자른다. */
export function parseAmountInput(text: string): number | null {
  const digits = text.replace(/[^0-9]/g, '');
  if (digits === '') return null;
  // 자릿수가 아주 긴 입력은 Number 로 바꾸다 정밀도를 잃으므로 먼저 상한으로 처리한다
  if (digits.replace(/^0+/, '').length > String(MAX_AMOUNT_WON).length) return MAX_AMOUNT_WON;
  return Math.min(Number(digits), MAX_AMOUNT_WON);
}

/** 현재 금액(없으면 0)에 step 을 더한다. 상한을 넘으면 상한에서 멈춘다. */
export function addAmount(current: number | null, step: number): number {
  return Math.min((current ?? 0) + step, MAX_AMOUNT_WON);
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
