import {
  formatDayLabel,
  formatNewsDate,
  summarizeInvestment,
  toHolding,
  toInvestmentNews,
  toStockDetail,
  toStockSummary,
} from './mappers';
import type { NewsSummaryResponse, ProductDetailResponse } from '@/features/investment/model/types';

const detailResponse: ProductDetailResponse = {
  investmentProductId: 1,
  name: '시원아이스',
  description: '아이스크림을 만드는 회사',
  category: 'STOCK',
  currentPrice: 10250,
  changeAmount: 150,
  previousPrice: 10100,
  isActive: true,
  updatedAt: '2026-09-16T00:05:00+09:00',
};

describe('formatNewsDate / formatDayLabel', () => {
  it('ISO 일시에서 표시용 날짜를 만든다 (타임존 파싱 없이)', () => {
    expect(formatNewsDate('2026-09-14T00:05:00+09:00')).toBe('2026. 09. 14');
    expect(formatDayLabel('2026-09-14T00:05:00+09:00')).toBe('9/14');
    expect(formatDayLabel('2026-10-03T06:00:00+09:00')).toBe('10/03');
  });
});

describe('toStockSummary', () => {
  it('시드 회사는 고유 아이콘·업종 라벨을 붙이고 등락은 changeAmount 를 쓴다', () => {
    const summary = toStockSummary({
      investmentProductId: 1,
      name: '시원아이스',
      category: 'STOCK',
      currentPrice: 10250,
      changeAmount: 150,
      isActive: true,
    });

    expect(summary.id).toBe(1);
    expect(summary.category).toBe('아이스크림');
    expect(summary.change).toBe(150);
  });

  it('시드에 없는 회사는 카테고리 한글 라벨로 대체한다', () => {
    const summary = toStockSummary({
      investmentProductId: 99,
      name: '새로운회사',
      category: 'BOND',
      currentPrice: 5000,
      changeAmount: 0,
      isActive: true,
    });

    expect(summary.category).toBe('채권');
  });
});

describe('toStockDetail', () => {
  it('가격 이력을 x축 라벨이 붙은 차트 점으로 바꾼다', () => {
    const detail = toStockDetail(
      detailResponse,
      [
        { price: 10100, recordedAt: '2026-09-15T00:05:00+09:00' },
        { price: 10250, recordedAt: '2026-09-16T00:05:00+09:00' },
      ],
      true,
    );

    expect(detail.holding).toBe(true);
    expect(detail.changeFromYesterday).toBe(150);
    expect(detail.history).toEqual([
      { label: '9/15', price: 10100 },
      { label: '9/16', price: 10250 },
    ]);
  });

  // TEMP: 이력이 비어 있을 때 mockPriceHistory로 목업 랜덤워크를 채운다 (AI 서버 연동 전
  // 그래프 미리보기용). 값 자체는 랜덤이라 형태(길이·오늘 값)만 검증한다.
  it('이력이 비어 있으면 목업 랜덤워크로 차트를 채우고 마지막 점은 현재가와 같다', () => {
    const detail = toStockDetail(detailResponse, [], false);

    expect(detail.history).toHaveLength(14);
    expect(detail.history[detail.history.length - 1].price).toBe(10250);
    detail.history.forEach((point) => {
      expect(typeof point.label).toBe('string');
      expect(Number.isFinite(point.price)).toBe(true);
    });
  });
});

describe('toHolding / summarizeInvestment', () => {
  it('보유 행을 화면 타입으로 바꾸고 넣은 돈·번 돈을 합산한다', () => {
    const holdings = [
      toHolding({
        investmentProductId: 1,
        productName: '시원아이스',
        quantity: 1,
        buyPrice: 10000,
        currentPrice: 10250,
      }),
      toHolding({
        investmentProductId: 8,
        productName: '고소한빵',
        quantity: 1,
        buyPrice: 10000,
        currentPrice: 9800,
      }),
    ];

    expect(holdings[0].buyPrice).toBe(10000);
    expect(summarizeInvestment(holdings)).toEqual({
      investedPoints: 20000,
      earnedPoints: 50,
    });
  });
});

describe('toInvestmentNews', () => {
  const news: NewsSummaryResponse = {
    investmentNewsId: 3,
    title: '밀 가격 상승, 빵집의 고민',
    impact: 'NEGATIVE',
    relatedProducts: [{ investmentProductId: 8, productName: '고소한빵', category: 'STOCK' }],
    publishedAt: '2026-09-16T00:05:00+09:00',
  };

  it('관련 회사의 업종·이모지와 영향 방향 문구를 붙인다', () => {
    const view = toInvestmentNews(news);

    expect(view.id).toBe(3);
    expect(view.tag).toBe('제빵');
    expect(view.emoji).toBe('🥐');
    expect(view.body).toBe('가격이 내릴 수 있는 소식이에요.');
    expect(view.relatedProductName).toBe('고소한빵');
    expect(view.date).toBe('2026. 09. 16');
    expect(view.content).toBeUndefined();
  });

  it('관련 회사가 없어도 기본값으로 안전하게 표시한다', () => {
    const view = toInvestmentNews({ ...news, relatedProducts: [] });

    expect(view.tag).toBe('오늘의 소식');
    expect(view.emoji).toBe('📰');
    expect(view.relatedProductName).toBe('');
  });
});
