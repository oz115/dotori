import { mockHoldings } from '@/features/investment/model/mock';

import { summarizeHoldings } from './holdings';

describe('summarizeHoldings', () => {
  it('보유 종목의 개수·현재가 합·번 돈을 계산한다', () => {
    // 시원아이스 10,250(+150) + 고소한빵 9,800(-100) + 파워배터리 10,400(+200)
    expect(summarizeHoldings(mockHoldings)).toEqual({
      count: 3,
      totalValue: 30450,
      totalEarned: 250,
    });
  });

  it('보유 종목이 없으면 전부 0이다', () => {
    expect(summarizeHoldings([])).toEqual({ count: 0, totalValue: 0, totalEarned: 0 });
  });
});
