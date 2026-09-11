import type {
  InvestmentNews,
  InvestmentSummary,
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

export const mockStocks: StockSummary[] = [
  {
    id: 1,
    name: '튼튼 마트',
    category: '생활 소비',
    emoji: '🏪',
    currentPrice: 980,
    change: 20,
    trend: [900, 930, 910, 950, 940, 970, 980],
  },
  {
    id: 2,
    name: '태수 마트',
    category: '생활 소비',
    emoji: '🛒',
    currentPrice: 1120,
    change: 35,
    trend: [1000, 1040, 1020, 1080, 1060, 1085, 1120],
  },
  {
    id: 3,
    name: '새싹 에너지',
    category: '환경',
    emoji: '🌱',
    currentPrice: 860,
    change: -15,
    trend: [900, 890, 905, 880, 895, 875, 860],
  },
];
