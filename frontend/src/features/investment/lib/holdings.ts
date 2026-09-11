import type { HoldingsSummary, StockDetail } from '@/features/investment/model/types';

/**
 * 보유 화면 상단 합계 계산. "번 돈"은 아직 매입가 데이터가 없어 어제 대비 등락의 합으로
 * 어림한다 — 백엔드 투자 API가 평균 매입가를 주면 (현재가 - 매입가) 합으로 교체한다.
 */
export function summarizeHoldings(holdings: StockDetail[]): HoldingsSummary {
  return {
    count: holdings.length,
    totalValue: holdings.reduce((sum, holding) => sum + holding.currentPrice, 0),
    totalEarned: holdings.reduce((sum, holding) => sum + holding.changeFromYesterday, 0),
  };
}
