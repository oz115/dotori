import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react-native';
import type { ReactElement } from 'react';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { ParentInvestmentHoldings } from './ParentInvestmentHoldings';
import { ParentInvestmentMarket } from './ParentInvestmentMarket';
import { ParentInvestmentNews } from './ParentInvestmentNews';
import { ParentInvestmentTransactions } from './ParentInvestmentTransactions';

jest.mock('@/features/investment/api/investmentApi', () => ({
  investmentApi: {
    getHoldings: jest.fn(),
    getTransactions: jest.fn(),
    getNews: jest.fn(),
    getProducts: jest.fn(),
  },
}));

const DAY_MS = 86_400_000;
const daysAgo = (days: number) => new Date(Date.now() - days * DAY_MS).toISOString();

async function show(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

beforeEach(() => jest.clearAllMocks());

describe('ParentInvestmentTransactions', () => {
  const sale = (id: number, name: string, ago: number, profit: number) => ({
    investmentTransactionId: id,
    productName: name,
    transactionType: 'SELL' as const,
    quantity: 1,
    unitPrice: 1000 + profit,
    totalAmount: 1000 + profit,
    createdAt: daysAgo(ago),
    buyPrice: 1000,
    realizedProfit: profit,
  });

  const show11 = () =>
    show(<ParentInvestmentTransactions childId={11} childName="민준" onBack={jest.fn()} />);

  /** 기간 알약 → 시트에서 프리셋 칩 → 확인 */
  const pickPreset = async (chipLabel: string) => {
    await fireEvent.press(screen.getByLabelText('조회 기간 고르기'));
    await fireEvent.press(await screen.findByText(chipLabel));
    await fireEvent.press(screen.getByText('확인'));
  };

  beforeEach(() => {
    jest
      .mocked(investmentApi.getTransactions)
      .mockResolvedValue([
        sale(3, '고소한빵', 1, 300),
        sale(2, '튼튼건설', 40, -100),
        sale(1, '하늘항공', 100, 50),
      ]);
  });

  it('처음엔 기간 제한 없이 전체 거래를 보여주고 자녀를 childId 로 지정해 조회한다', async () => {
    await show11();
    expect(await screen.findByText('고소한빵')).toBeTruthy();
    expect(screen.getByText('튼튼건설')).toBeTruthy();
    expect(screen.getByText('하늘항공')).toBeTruthy();
    expect(screen.getByText('전체 3건')).toBeTruthy();
    expect(screen.getByText('민준의 거래 내역')).toBeTruthy();
    expect(screen.getByText('전체 기간')).toBeTruthy();
    expect(investmentApi.getTransactions).toHaveBeenCalledWith(11);
  });

  it('1개월을 고르면 최근 1개월 거래만 보여준다', async () => {
    await show11();
    await screen.findByText('고소한빵');
    await pickPreset('1개월');

    expect(await screen.findByText('최근 1개월')).toBeTruthy();
    expect(screen.getByText('고소한빵')).toBeTruthy();
    expect(screen.queryByText('튼튼건설')).toBeNull();
    expect(screen.getByText('이 기간 1건')).toBeTruthy();
  });

  it('3개월을 고르면 그 안의 거래까지 보여준다', async () => {
    await show11();
    await screen.findByText('고소한빵');
    await pickPreset('3개월');

    expect(await screen.findByText('최근 3개월')).toBeTruthy();
    expect(screen.getByText('튼튼건설')).toBeTruthy();
    expect(screen.queryByText('하늘항공')).toBeNull();
    expect(screen.getByText('이 기간 2건')).toBeTruthy();
  });

  it('고른 기간에 거래가 없으면 안내하고, 전체로 돌아오면 다 보인다', async () => {
    jest.mocked(investmentApi.getTransactions).mockResolvedValue([sale(1, '튼튼건설', 40, -100)]);
    await show11();
    await screen.findByText('튼튼건설');
    await pickPreset('1개월');
    expect(await screen.findByText('이 기간에는 사고판 주식이 없어요.')).toBeTruthy();

    await pickPreset('전체');
    expect(await screen.findByText('전체 1건')).toBeTruthy();
  });

  it('거래가 하나도 없으면 전체에서 기록이 없다고 알려준다', async () => {
    jest.mocked(investmentApi.getTransactions).mockResolvedValue([]);
    await show11();
    expect(await screen.findByText('아직 사고판 기록이 없어요.')).toBeTruthy();
  });

  it('childId 가 없으면 부모 본인 거래를 묻지 않고 안내만 보여준다', async () => {
    await show(
      <ParentInvestmentTransactions childId={undefined} childName={undefined} onBack={jest.fn()} />,
    );
    expect(await screen.findByText('어느 아이의 거래인지 알 수 없어요.')).toBeTruthy();
    expect(investmentApi.getTransactions).not.toHaveBeenCalled();
  });
});

describe('ParentInvestmentHoldings', () => {
  it('자녀의 보유 주식을 childId 로 조회해 보여주고, 누르면 종목으로 넘긴다', async () => {
    jest.mocked(investmentApi.getHoldings).mockResolvedValue([
      {
        investmentProductId: 1,
        productName: '고소한빵',
        quantity: 1,
        buyPrice: 1000,
        currentPrice: 1200,
      },
    ]);
    const onPressStock = jest.fn();
    await show(
      <ParentInvestmentHoldings
        childId={11}
        childName="민준"
        onBack={jest.fn()}
        onPressStock={onPressStock}
      />,
    );
    await fireEvent.press(await screen.findByText('고소한빵'));
    expect(investmentApi.getHoldings).toHaveBeenCalledWith(11);
    expect(onPressStock).toHaveBeenCalledWith(1);
  });

  it('childId 가 없으면 조회하지 않는다', async () => {
    await show(
      <ParentInvestmentHoldings
        childId={undefined}
        childName={undefined}
        onBack={jest.fn()}
        onPressStock={jest.fn()}
      />,
    );
    expect(await screen.findByText('어느 아이의 보유 주식인지 알 수 없어요.')).toBeTruthy();
    expect(investmentApi.getHoldings).not.toHaveBeenCalled();
  });
});

describe('ParentInvestmentNews', () => {
  it('오늘의 뉴스를 목록으로 보여주고 누르면 본문으로 넘긴다', async () => {
    jest.mocked(investmentApi.getNews).mockResolvedValue([
      {
        investmentNewsId: 5,
        title: '밀 가격이 올랐어요',
        impact: 'NEGATIVE',
        relatedProducts: [],
        publishedAt: '2026-09-21T00:05:00+09:00',
      },
    ]);
    const onPressNews = jest.fn();
    await show(<ParentInvestmentNews onBack={jest.fn()} onPressNews={onPressNews} />);
    await fireEvent.press(await screen.findByText('밀 가격이 올랐어요'));
    expect(onPressNews).toHaveBeenCalledWith(5);
  });
});

describe('ParentInvestmentMarket', () => {
  it('종목 시세를 보여주고 누르면 상세로 넘긴다', async () => {
    jest.mocked(investmentApi.getProducts).mockResolvedValue([
      {
        investmentProductId: 1,
        name: '고소한빵',
        category: 'STOCK',
        currentPrice: 1200,
        changeAmount: 100,
        isActive: true,
      },
    ]);
    const onPressStock = jest.fn();
    await show(<ParentInvestmentMarket onBack={jest.fn()} onPressStock={onPressStock} />);
    await fireEvent.press(await screen.findByText('고소한빵'));
    expect(onPressStock).toHaveBeenCalledWith(1);
    expect(screen.getByText(/하루에 한 번, 자정 직후에 바뀌어요/)).toBeTruthy();
  });
});
