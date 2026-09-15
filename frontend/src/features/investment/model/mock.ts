import type {
  InvestmentNews,
  InvestmentSummary,
  PricePoint,
  StockDetail,
  StockSummary,
} from '@/features/investment/model/types';

/**
 * 백엔드 투자 API 연동 전까지 화면을 채우는 목업 데이터.
 * 회사 10개는 시드(backend 02-seed-investment-products.sql, 노션 「회사 리스트」)와
 * 같은 이름·설명이고, 가격은 시드 시작가(10000) 언저리로 흩어 둔 화면용 값이다.
 * API가 생기면 api/ + TanStack Query 훅으로 교체하고 이 파일은 삭제한다.
 */

// 종목 아이콘 — Figma 투자 종목 시트에서 추출한 이미지 (시드와 같은 순서)
const COMPANY_IMAGES = {
  ice: require('@/assets/images/invest/companies/ice.png'),
  farm: require('@/assets/images/invest/companies/farm.png'),
  travel: require('@/assets/images/invest/companies/travel.png'),
  world: require('@/assets/images/invest/companies/world.png'),
  air: require('@/assets/images/invest/companies/air.png'),
  delivery: require('@/assets/images/invest/companies/delivery.png'),
  construction: require('@/assets/images/invest/companies/construction.png'),
  bread: require('@/assets/images/invest/companies/bread.png'),
  car: require('@/assets/images/invest/companies/car.png'),
  battery: require('@/assets/images/invest/companies/battery.png'),
} as const;

export const mockInvestmentSummary: InvestmentSummary = {
  investedPoints: 7300,
  earnedPoints: 7300,
};

// 설계(하루 상승 2건 + 하락 1건)에 맞춘 오늘의 뉴스. 각 회사의 대표 경제 요인
// (news_context)을 따르고, 등락 방향은 아래 종목 목업의 change 와 맞춘다.
export const mockInvestmentNews: InvestmentNews[] = [
  {
    id: 1,
    tag: '기온',
    title: '무더위 계속,\n아이스크림 불티',
    body: '더운 날이 이어져 아이스크림 판매가 늘었어요.',
    content:
      '무더운 날이 계속되면서 시원아이스의 아이스크림을 찾는 사람이 부쩍 늘었어요. ' +
      '공장이 바쁘게 돌아가고 있고, 가게마다 아이스크림이 불티나게 팔리고 있대요.',
    impact: 'POSITIVE',
    relatedProductName: '시원아이스',
    date: '2026. 09. 14',
    emoji: '🍦',
  },
  {
    id: 2,
    tag: '주문량',
    title: '온라인 주문 급증,\n택배 상자 산더미',
    body: '온라인 주문이 늘어 배송할 상자가 많아졌어요.',
    content:
      '온라인으로 물건을 주문하는 사람이 크게 늘었어요. 번개택배의 물류 창고에는 ' +
      '배송을 기다리는 상자가 산더미처럼 쌓였고, 배송 기사님들이 바쁘게 움직이고 있어요.',
    impact: 'POSITIVE',
    relatedProductName: '번개택배',
    date: '2026. 09. 14',
    emoji: '📦',
  },
  {
    id: 3,
    tag: '밀 가격',
    title: '밀 가격 상승,\n빵집의 고민',
    body: '밀 가격이 올라 빵 만드는 비용이 늘었어요.',
    content:
      '빵의 재료인 밀 가격이 올랐어요. 고소한빵은 빵을 만드는 데 돈이 더 들게 되어서 ' +
      '걱정이 많아요. 밀 가격이 다시 내려가기를 기다리고 있답니다.',
    impact: 'NEGATIVE',
    relatedProductName: '고소한빵',
    date: '2026. 09. 14',
    emoji: '🥖',
  },
];

const DAY_LABELS = ['9/08', '9/09', '9/10', '9/11', '9/12', '9/13', '9/14'];

const toHistory = (prices: number[]): PricePoint[] =>
  prices.map((price, index) => ({ label: DAY_LABELS[index], price }));

/** 시드(노션 「회사 리스트」)와 같은 회사 10개. 목업 값의 원본은 여기 하나다. */
const COMPANIES = [
  {
    id: 1,
    name: '시원아이스',
    category: '아이스크림',
    image: COMPANY_IMAGES.ice,
    description: '아이스크림을 만들어 정해진 가격에 판매해요.',
    holding: true,
    trend: [9800, 9900, 9850, 10000, 10050, 10100, 10250],
  },
  {
    id: 2,
    name: '푸른들농업',
    category: '농업',
    image: COMPANY_IMAGES.farm,
    description: '밀과 채소를 재배해 미리 약속한 가격으로 판매해요.',
    holding: false,
    trend: [10000, 10050, 9980, 10020, 10080, 10100, 10150],
  },
  {
    id: 3,
    name: '두근여행',
    category: '여행',
    image: COMPANY_IMAGES.travel,
    description: '가족이 이용할 수 있는 국내 여행 상품을 판매해요.',
    holding: false,
    trend: [10200, 10150, 10100, 10050, 10080, 10000, 9920],
  },
  {
    id: 4,
    name: '신나는월드',
    category: '놀이공원',
    image: COMPANY_IMAGES.world,
    description: '야외 놀이기구가 많은 놀이공원을 운영해요.',
    holding: false,
    trend: [9900, 9950, 10000, 10080, 10120, 10180, 10300],
  },
  {
    id: 5,
    name: '하늘항공',
    category: '항공',
    image: COMPANY_IMAGES.air,
    description: '승객을 태우는 비행기를 운항해요.',
    holding: false,
    trend: [10100, 10050, 10000, 9950, 9980, 9900, 9750],
  },
  {
    id: 6,
    name: '번개택배',
    category: '택배',
    image: COMPANY_IMAGES.delivery,
    description: '온라인으로 주문한 상품을 배송해요.',
    holding: false,
    trend: [9900, 10000, 10100, 10150, 10200, 10250, 10450],
  },
  {
    id: 7,
    name: '튼튼건설',
    category: '건설',
    image: COMPANY_IMAGES.construction,
    description: '새 아파트를 지어 정해진 가격에 판매해요.',
    holding: false,
    trend: [10050, 10020, 10080, 10040, 10000, 10000, 9940],
  },
  {
    id: 8,
    name: '고소한빵',
    category: '제빵',
    image: COMPANY_IMAGES.bread,
    description: '밀가루로 빵을 만들어 정해진 가격에 판매해요.',
    holding: true,
    trend: [10000, 10050, 10020, 9980, 9950, 9900, 9800],
  },
  {
    id: 9,
    name: '씽씽자동차',
    category: '자동차',
    image: COMPANY_IMAGES.car,
    description: '전기차를 주로 만들어 정해진 가격에 판매해요.',
    holding: false,
    trend: [10000, 9980, 10050, 10100, 10060, 10090, 10180],
  },
  {
    id: 10,
    name: '파워배터리',
    category: '배터리',
    image: COMPANY_IMAGES.battery,
    description: '전기차에 들어가는 배터리를 만들어요.',
    holding: true,
    trend: [10000, 10100, 10150, 10120, 10180, 10200, 10400],
  },
];

const lastOf = (trend: number[]): number => trend[trend.length - 1];
const changeOf = (trend: number[]): number => lastOf(trend) - trend[trend.length - 2];

export const mockStocks: StockSummary[] = COMPANIES.map((company) => ({
  id: company.id,
  name: company.name,
  category: company.category,
  image: company.image,
  currentPrice: lastOf(company.trend),
  change: changeOf(company.trend),
  trend: company.trend,
}));

/** 리스트(mockStocks)와 같은 종목의 상세 — 같은 원본에서 만들어 어긋날 수 없다. */
export const mockStockDetails: StockDetail[] = COMPANIES.map((company) => ({
  id: company.id,
  name: company.name,
  category: company.category,
  image: company.image,
  description: company.description,
  holding: company.holding,
  currentPrice: lastOf(company.trend),
  changeFromYesterday: changeOf(company.trend),
  history: toHistory(company.trend),
}));

export function findMockStockDetail(stockId: number): StockDetail | undefined {
  return mockStockDetails.find((detail) => detail.id === stockId);
}

/** 보유 화면 목업 — 보유중 종목만. 화면 상단 합계는 lib/holdings 가 여기서 계산한다. */
export const mockHoldings: StockDetail[] = mockStockDetails.filter((detail) => detail.holding);
