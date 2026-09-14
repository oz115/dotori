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

// 설계(상승 2건 + 하락 1건)에 맞춰 하락 뉴스 하나를 두고, 종목 목업(mockStocks)의
// 등락 방향과도 맞춘다 — 새싹 에너지·미래 로봇은 오르고 태수 마트는 내리는 중.
export const mockInvestmentNews: InvestmentNews[] = [
  {
    id: 1,
    tag: '환경',
    title: '새싹 에너지,\n친환경 발전소 확대',
    body: '깨끗한 에너지 기업의 성장세가 지속되고 있어요.',
    content:
      '요즘 깨끗한 에너지를 찾는 사람들이 부쩍 늘었어요. 새싹 에너지는 새 태양광 발전소를 ' +
      '지어서 더 많은 전기를 만들 수 있게 됐답니다. 지구도 지키고 회사도 쑥쑥 크고 있어요!',
    impact: 'POSITIVE',
    relatedProductName: '새싹 에너지',
    date: '2025. 09. 07',
    emoji: '🌍',
  },
  {
    id: 2,
    tag: '기술',
    title: '미래 로봇,\n공장에서 대활약',
    body: '로봇 기술 회사들이 새 공장을 열고 있어요.',
    content:
      '미래 로봇이 만든 집안일 도우미 로봇이 큰 인기예요. 주문이 많아져서 새 공장을 ' +
      '열었고, 로봇 친구들이 더 많은 집으로 갈 준비를 하고 있어요.',
    impact: 'POSITIVE',
    relatedProductName: '미래 로봇',
    date: '2025. 09. 07',
    emoji: '🤖',
  },
  {
    id: 3,
    tag: '생활',
    title: '태수 마트,\n손님이 줄었어요',
    body: '근처에 큰 마트가 새로 생겨서 걱정이에요.',
    content:
      '태수 마트 근처에 아주 큰 마트가 새로 문을 열었어요. 손님들이 새 마트로 가면서 ' +
      '태수 마트의 매출이 줄고 있대요. 다시 손님을 모을 방법을 고민하고 있어요.',
    impact: 'NEGATIVE',
    relatedProductName: '태수 마트',
    date: '2025. 09. 07',
    emoji: '🛒',
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
