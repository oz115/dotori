import { previewHoldings, summarizeHoldings } from './holdings';
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

describe('previewHoldings', () => {
  it('기본 3개까지는 전부 보여주고 나머지는 없다', () => {
    expect(previewHoldings(holdings)).toEqual({
      items: [
        { id: 1, name: '시원아이스', image: IMAGE },
        { id: 8, name: '고소한빵', image: IMAGE },
        { id: 10, name: '파워배터리', image: IMAGE },
      ],
      restCount: 0,
    });
  });

  it('한도를 넘으면 앞쪽만 두고 나머지를 센다', () => {
    const many: Holding[] = [
      ...holdings,
      { id: 11, name: '반짝전자', image: IMAGE, quantity: 1, buyPrice: 9000, currentPrice: 9500 },
      { id: 12, name: '튼튼건설', image: IMAGE, quantity: 1, buyPrice: 8000, currentPrice: 7900 },
    ];

    expect(previewHoldings(many).items.map((item) => item.name)).toEqual([
      '시원아이스',
      '고소한빵',
      '파워배터리',
    ]);
    expect(previewHoldings(many).restCount).toBe(2);
  });

  it('아이콘을 이름과 함께 넘긴다 — 화면이 이름으로 다시 찾지 않게 한다', () => {
    const withIcon: Holding[] = [
      { id: 1, name: '시원아이스', image: 42, quantity: 1, buyPrice: 100, currentPrice: 110 },
    ];

    expect(previewHoldings(withIcon).items[0]).toEqual({
      id: 1,
      name: '시원아이스',
      image: 42,
    });
  });

  it('보유가 없으면 빈 미리보기다', () => {
    expect(previewHoldings([])).toEqual({ items: [], restCount: 0 });
  });
});
