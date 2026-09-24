import type { PointTransactionReason } from '@/features/wallet/model/types';

/**
 * 포인트 거래 사유의 아이용 표기. description 이 비어 있는 거래도 이 라벨로
 * "무슨 일로 움직였는지"는 항상 보여줄 수 있다.
 */
const REASON_LABELS: Record<PointTransactionReason, string> = {
  CHARGE: '도토리 충전',
  GRANT: '부모님 용돈',
  ALLOWANCE: '정기 용돈',
  PURCHASE: '상점 구매',
  CASHOUT: '환전',
  SAVINGS_DEPOSIT: '저축 넣기',
  SAVINGS_WITHDRAWAL: '저축 찾기',
  INVESTMENT_BUY: '주식 사기',
  INVESTMENT_SELL: '주식 팔기',
  QUIZ_REWARD: '퀴즈 보상',
  CHORE_REWARD: '미션 보상',
  REFUND: '돌려받기',
};

export function pointTransactionReasonLabel(reason: PointTransactionReason): string {
  // 서버에 사유가 새로 생겨도 화면이 깨지지 않게 원문으로 폴백한다
  return REASON_LABELS[reason] ?? reason;
}
