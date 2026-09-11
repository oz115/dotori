import type {
  InvestmentNews,
  InvestmentSummary,
  PricePoint,
  StockDetail,
  StockSummary,
} from '@/features/investment/model/types';

/**
 * 백엔드 투자 API 연동 전까지 화면을 채우는 목업 데이터.
 * API가 생기면 api/ + TanStack Query 훅으로 교체하고 이 파일은 삭제한다.
 */

export const mockInvestmentSummary: InvestmentSummary = {
  investedPoints: 7300,
  earnedPoints: 7300,
};

export const mockInvestmentNews: InvestmentNews[] = [
  {
    id: 1,
    tag: '환경',
    title: '새싹 에너지,\n친환경 발전소 확대',
    body: '깨끗한 에너지 기업의 성장세가 지속되고 있어요.',
    date: '2025. 09. 07',
    emoji: '🌍',
  },
  {
    id: 2,
    tag: '기술',
    title: '로봇 친구들,\n공장에서 대활약',
    body: '로봇 기술 회사들이 새 공장을 열고 있어요.',
    date: '2025. 09. 07',
    emoji: '🤖',
  },
  {
    id: 3,
    tag: '생활',
    title: '간식 회사,\n신제품 인기몰이',
    body: '새로 나온 간식이 친구들 사이에서 인기예요.',
    date: '2025. 09. 07',
    emoji: '🍪',
  },
];

const DAY_LABELS = ['9/01', '9/02', '9/03', '9/04', '9/05', '9/06', '9/07'];

const toHistory = (prices: number[]): PricePoint[] =>
  prices.map((price, index) => ({ label: DAY_LABELS[index], price }));

export const mockStocks: StockSummary[] = [
  {
    id: 1,
    name: '튼튼 마트',
    category: '생활 소비',
    emoji: '🏪',
    currentPrice: 980,
    change: 20,
    trend: [900, 930, 910, 950, 940, 960, 980],
  },
  {
    id: 2,
    name: '태수 마트',
    category: '생활 소비',
    emoji: '🛒',
    currentPrice: 860,
    change: -15,
    trend: [900, 890, 905, 880, 895, 875, 860],
  },
  {
    id: 3,
    name: '새싹 에너지',
    category: '친환경 기업',
    emoji: '🌍',
    currentPrice: 1120,
    change: 40,
    trend: [650, 760, 720, 850, 930, 1080, 1120],
  },
  {
    id: 4,
    name: '미래 로봇',
    category: '기술',
    emoji: '🤖',
    currentPrice: 1450,
    change: 120,
    trend: [1100, 1180, 1150, 1260, 1300, 1330, 1450],
  },
];

/** 리스트(mockStocks)와 같은 종목의 상세. id·현재가·등락이 서로 어긋나면 안 된다. */
export const mockStockDetails: StockDetail[] = [
  {
    id: 1,
    name: '튼튼 마트',
    category: '생활 소비',
    emoji: '🏪',
    description: '우리 동네 알뜰 마트예요. 생활에 필요한 물건을 팔아요.',
    holding: true,
    currentPrice: 980,
    changeFromYesterday: 20,
    history: toHistory([900, 930, 910, 950, 940, 960, 980]),
  },
  {
    id: 2,
    name: '태수 마트',
    category: '생활 소비',
    emoji: '🛒',
    description: '싱싱한 먹거리를 파는 마트예요. 요즘은 손님이 조금 줄었어요.',
    holding: false,
    currentPrice: 860,
    changeFromYesterday: -15,
    history: toHistory([900, 890, 905, 880, 895, 875, 860]),
  },
  {
    id: 3,
    name: '새싹 에너지',
    category: '친환경 기업',
    emoji: '🌍',
    description: '지구를 위한 깨끗한 에너지, 더 밝은 내일을 만들어요.',
    holding: true,
    currentPrice: 1120,
    changeFromYesterday: 40,
    history: toHistory([650, 760, 720, 850, 930, 1080, 1120]),
  },
  {
    id: 4,
    name: '미래 로봇',
    category: '기술',
    emoji: '🤖',
    description: '집안일을 도와주는 로봇을 만드는 회사예요.',
    holding: true,
    currentPrice: 1450,
    changeFromYesterday: 120,
    history: toHistory([1100, 1180, 1150, 1260, 1300, 1330, 1450]),
  },
];

export function findMockStockDetail(stockId: number): StockDetail | undefined {
  return mockStockDetails.find((detail) => detail.id === stockId);
}

/** 보유 화면 목업 — 보유중 종목만. 화면 상단 합계는 lib/holdings 가 여기서 계산한다. */
export const mockHoldings: StockDetail[] = mockStockDetails.filter((detail) => detail.holding);
