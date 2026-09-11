import { mockHoldings } from '@/features/investment/model/mock';

import { summarizeHoldings } from './holdings';

describe('summarizeHoldings', () => {
  it('보유 종목의 개수·현재가 합·번 돈을 계산한다 (Figma 시안 케이스)', () => {
    // 새싹 에너지 1,120(+40) + 튼튼 마트 980(+20) + 미래 로봇 1,450(+120)
    expect(summarizeHoldings(mockHoldings)).toEqual({
      count: 3,
      totalValue: 3550,
      totalEarned: 180,
    });
  });

  it('보유 종목이 없으면 전부 0이다', () => {
    expect(summarizeHoldings([])).toEqual({ count: 0, totalValue: 0, totalEarned: 0 });
  });
});
