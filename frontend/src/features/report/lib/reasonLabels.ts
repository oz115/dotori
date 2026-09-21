import type { IncomeType, SpendingType } from '@/features/report/model/types';

/**
 * 사유 enum 을 화면 문구로 바꾼다.
 *
 * <p>서버는 `point_transactions.transaction_type` 라벨을 그대로 내보낸다(CHORE_REWARD 등).
 * 지갑 거래내역(WLT-06)도 같은 값을 쓰므로, 그쪽 화면이 생기면 이 표를 같이 쓰면 된다.
 *
 * <p>아이가 읽는 화면이 아니라 부모 화면이지만, 문구는 앱 전체 어투에 맞춰 짧게 둔다.
 */

const INCOME_LABELS: Record<IncomeType, string> = {
  CHARGE: '충전',
  GRANT: '용돈 받기',
  ALLOWANCE: '정기 용돈',
  CHORE_REWARD: '미션 보상',
  QUIZ_REWARD: '퀴즈 보상',
  SAVINGS_WITHDRAWAL: '예적금 수령',
  INVESTMENT_SELL: '투자 매도',
  REFUND: '환불',
};

const SPENDING_LABELS: Record<SpendingType, string> = {
  PURCHASE: '상점 구매',
  CASHOUT: '환전',
  SAVINGS_DEPOSIT: '예적금 납입',
  INVESTMENT_BUY: '투자 매수',
};

/** 모르는 값이 와도 화면이 비지 않게 enum 이름을 그대로 보여 준다 */
export function incomeTypeLabel(type: IncomeType): string {
  return INCOME_LABELS[type] ?? type;
}

export function spendingTypeLabel(type: SpendingType): string {
  return SPENDING_LABELS[type] ?? type;
}
