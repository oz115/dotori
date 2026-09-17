import type { Holding, HoldingsSummary } from '@/features/investment/model/types';

/**
 * 보유 화면 상단 합계 계산. "번 돈"은 (현재가 - 산 가격) × 수량의 합 —
 * 종목당 1주 규칙이라 사실상 현재가와 산 가격의 차이 합이다.
 */
export function summarizeHoldings(holdings: Holding[]): HoldingsSummary {
  return {
    count: holdings.length,
    totalValue: holdings.reduce((sum, h) => sum + h.currentPrice * h.quantity, 0),
    totalEarned: holdings.reduce((sum, h) => sum + (h.currentPrice - h.buyPrice) * h.quantity, 0),
  };
}
