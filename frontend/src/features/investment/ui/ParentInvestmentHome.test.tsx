import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react-native';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { ParentInvestmentHome } from './ParentInvestmentHome';

jest.mock('@/features/investment/api/investmentApi', () => ({
  investmentApi: { getHoldings: jest.fn(), getTransactions: jest.fn() },
}));
// 화면 포커스 훅은 네비게이션 컨테이너가 필요해서 테스트에선 no-op 으로 둔다 (다른 화면 테스트와 같다)
jest.mock('expo-router', () => ({
  ...jest.requireActual('expo-router'),
  useFocusEffect: () => {},
}));

const DAY_MS = 86_400_000;
const daysAgo = (days: number) => new Date(Date.now() - days * DAY_MS).toISOString();

const children = [
  { childId: 11, name: '민준' },
  { childId: 12, name: '서윤' },
];

const handlers = {
  onPressStock: jest.fn(),
  onPressHoldings: jest.fn(),
  onPressTransactions: jest.fn(),
  onPressMarket: jest.fn(),
  onPressNews: jest.fn(),
  onPressNewsReview: jest.fn(),
};

const holding = (id: number, name: string, buyPrice = 1000, currentPrice = 1200) => ({
  investmentProductId: id,
  productName: name,
  quantity: 1,
  buyPrice,
  currentPrice,
});

async function show(childList = children) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={client}>
      <ParentInvestmentHome childList={childList} {...handlers} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  jest.mocked(investmentApi.getHoldings).mockResolvedValue([holding(1, '고소한빵')]);
  jest.mocked(investmentApi.getTransactions).mockResolvedValue([
    {
      investmentTransactionId: 2,
      productName: '고소한빵',
      transactionType: 'SELL',
      quantity: 1,
      unitPrice: 1300,
      totalAmount: 1300,
      createdAt: daysAgo(1),
      buyPrice: 1000,
      realizedProfit: 300,
    },
    {
      investmentTransactionId: 1,
      productName: '튼튼건설',
      transactionType: 'BUY',
      quantity: 1,
      unitPrice: 800,
      totalAmount: 800,
      createdAt: daysAgo(30),
    },
  ]);
});

it('첫째 자녀의 보유·거래를 childId 로 지정해 조회한다 — 부모 본인 것을 묻지 않는다', async () => {
  await show();
  expect(await screen.findByText('민준의 투자')).toBeTruthy();
  expect(investmentApi.getHoldings).toHaveBeenCalledWith(11);
  expect(investmentApi.getTransactions).toHaveBeenCalledWith(11);
});

it('요약 띠에 지금 가치·산 값 대비 수익·최근 7일 판매 수익을 묶어 보여주고 사기·팔기는 없다', async () => {
  await show();
  expect(await screen.findByText('들고 있는 주식 1개의 지금 가치')).toBeTruthy();
  expect(screen.getByText('산 값보다 200 도토리 올랐어요')).toBeTruthy();
  expect(await screen.findByText('+300 도토리')).toBeTruthy();
  expect(screen.queryByText('사기')).toBeNull();
  expect(screen.queryByText('팔기')).toBeNull();
});

it('거래 카드에는 최근 7일 거래 건수만 적고, 30일 전 거래는 세지 않는다', async () => {
  await show();
  expect(await screen.findByText('최근 7일 1건')).toBeTruthy();
});

it('보유 주식은 앞 3개만 미리 보여주고, 넘치면 전체 보기로 페이지를 넘긴다', async () => {
  jest
    .mocked(investmentApi.getHoldings)
    .mockResolvedValue([
      holding(1, '고소한빵'),
      holding(2, '튼튼건설'),
      holding(3, '하늘항공'),
      holding(4, '번개택배'),
    ]);
  await show();
  expect(await screen.findByText('보유 주식 4개')).toBeTruthy();
  expect(screen.getByText('하늘항공')).toBeTruthy();
  expect(screen.queryByText('번개택배')).toBeNull();
  await fireEvent.press(screen.getByText('전체 보기'));
  expect(handlers.onPressHoldings).toHaveBeenCalledWith(11);
});

it('보유 주식이 3개 이하면 전체 보기를 보여주지 않는다', async () => {
  await show();
  await screen.findByText('보유 주식 1개');
  expect(screen.queryByText('전체 보기')).toBeNull();
});

it('색 카드 4장이 각 페이지로 넘어간다 — 거래 내역은 고른 자녀 기준이다', async () => {
  await show();
  await screen.findByText('민준의 투자');

  await fireEvent.press(screen.getByText('거래 내역'));
  expect(handlers.onPressTransactions).toHaveBeenCalledWith(11);
  await fireEvent.press(screen.getByText('오늘의 뉴스'));
  expect(handlers.onPressNews).toHaveBeenCalled();
  await fireEvent.press(screen.getByText('주식 시세'));
  expect(handlers.onPressMarket).toHaveBeenCalledWith(11);
  await fireEvent.press(screen.getByText('지난 뉴스 해설'));
  expect(handlers.onPressNewsReview).toHaveBeenCalled();
});

it('둘째 자녀 칩을 누르면 그 자녀 기준으로 다시 조회하고 이동에도 그 자녀를 쓴다', async () => {
  await show();
  await screen.findByText('민준의 투자');
  await fireEvent.press(screen.getByText('서윤'));
  expect(await screen.findByText('서윤의 투자')).toBeTruthy();
  expect(investmentApi.getHoldings).toHaveBeenCalledWith(12);
  expect(investmentApi.getTransactions).toHaveBeenCalledWith(12);
  await fireEvent.press(screen.getByText('거래 내역'));
  expect(handlers.onPressTransactions).toHaveBeenCalledWith(12);
});

it('자녀가 한 명이면 고르는 칩을 보여주지 않는다', async () => {
  await show([children[0]]);
  await screen.findByText('민준의 투자');
  expect(screen.queryByText('서윤')).toBeNull();
});

it('자녀가 없으면 안내만 띄우고 보유·거래는 조회하지 않으며, 거래 내역 카드도 누를 수 없다', async () => {
  await show([]);
  expect(await screen.findByText(/아직 연결된 아이가 없어요/)).toBeTruthy();
  expect(investmentApi.getHoldings).not.toHaveBeenCalled();
  expect(investmentApi.getTransactions).not.toHaveBeenCalled();
  // 뉴스·시세·해설은 가족 단위라 자녀가 없어도 들어갈 수 있다
  await fireEvent.press(screen.getByText('오늘의 뉴스'));
  expect(handlers.onPressNews).toHaveBeenCalled();
  await fireEvent.press(screen.getByText('거래 내역'));
  expect(handlers.onPressTransactions).not.toHaveBeenCalled();
});

it('보유 조회가 실패해도 페이지로 들어가는 카드는 계속 보여준다', async () => {
  jest.mocked(investmentApi.getHoldings).mockRejectedValue(new Error('offline'));
  await show();
  expect(
    await screen.findByText('정보를 불러오지 못했어요. 연결을 확인한 뒤 다시 시도해 주세요.'),
  ).toBeTruthy();
  expect(screen.getByText('주식 시세')).toBeTruthy();
});
