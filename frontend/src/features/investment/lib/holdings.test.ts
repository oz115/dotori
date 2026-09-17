import { summarizeHoldings } from './holdings';
import type { Holding } from '@/features/investment/model/types';

const IMAGE = 0; // require() 결과 자리 — 계산 로직만 검증하므로 숫자 소스로 충분

const holdings: Holding[] = [
  { id: 1, name: '시원아이스', image: IMAGE, quantity: 1, buyPrice: 10100, currentPrice: 10250 },
  { id: 8, name: '고소한빵', image: IMAGE, quantity: 1, buyPrice: 9900, currentPrice: 9800 },
  { id: 10, name: '파워배터리', image: IMAGE, quantity: 1, buyPrice: 10200, currentPrice: 10400 },
];

describe('summarizeHoldings', () => {
  it('보유 수·지금 가치 합·번 돈(산 가격 대비)을 계산한다', () => {
    expect(summarizeHoldings(holdings)).toEqual({
      count: 3,
      totalValue: 30450,
      totalEarned: 150 - 100 + 200,
    });
  });

  it('보유가 없으면 전부 0이다', () => {
    expect(summarizeHoldings([])).toEqual({ count: 0, totalValue: 0, totalEarned: 0 });
  });
});
