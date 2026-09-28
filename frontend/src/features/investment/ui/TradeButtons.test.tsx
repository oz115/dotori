import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import type { ReactElement } from 'react';
import { Alert } from 'react-native';

import { investmentApi } from '@/features/investment/api/investmentApi';
import type { OrderResponse, StockDetail } from '@/features/investment/model/types';

import { TradeButtons } from './TradeButtons';

jest.mock('@/features/investment/api/investmentApi', () => ({
  investmentApi: {
    buy: jest.fn(),
    sell: jest.fn(),
  },
}));

const detail: StockDetail = {
  id: 1,
  name: '시원아이스',
  category: '아이스크림',
  image: 0,
  description: '아이스크림을 만드는 회사',
  holding: false,
  currentPrice: 10250,
  changeFromYesterday: 150,
  history: [{ label: '오늘', price: 10250 }],
};

const order: OrderResponse = {
  investmentTransactionId: 1,
  transactionType: 'BUY',
  quantity: 1,
  unitPrice: 10250,
  totalAmount: 10250,
  pointBalanceAfter: 4750,
  createdAt: '2026-09-16T12:00:00+09:00',
};

function renderWithQuery(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

/** Alert 확인 다이얼로그에서 특정 버튼을 눌렀다고 치는 헬퍼 */
function pressAlertButton(label: string) {
  const call = jest.mocked(Alert.alert).mock.calls.at(-1);
  const button = call?.[2]?.find((b) => b.text === label);
  button?.onPress?.();
}

describe('TradeButtons', () => {
  beforeEach(() => {
    jest.spyOn(Alert, 'alert').mockImplementation(jest.fn());
    jest.clearAllMocks();
  });

  it('미보유 종목은 사기만 활성화된다', async () => {
    await renderWithQuery(<TradeButtons detail={detail} />);

    expect(screen.getByText('사기').props.accessibilityState?.disabled).toBeFalsy();
    fireEvent.press(screen.getByText('팔기'));
    expect(jest.mocked(Alert.alert)).not.toHaveBeenCalled();
  });

  it('사기 확인 후 매수 API 를 호출하고 결과를 안내한다', async () => {
    jest.mocked(investmentApi.buy).mockResolvedValue(order);
    await renderWithQuery(<TradeButtons detail={detail} />);

    fireEvent.press(screen.getByText('사기'));
    expect(jest.mocked(Alert.alert)).toHaveBeenCalledWith(
      '주식 사기',
      expect.stringContaining('시원아이스'),
      expect.anything(),
    );

    pressAlertButton('살래요');
    await waitFor(() => expect(jest.mocked(investmentApi.buy)).toHaveBeenCalledWith(1));
    await waitFor(() =>
      expect(jest.mocked(Alert.alert)).toHaveBeenCalledWith(
        '샀어요!',
        expect.stringContaining('4,750'),
      ),
    );
  });

  it('도토리가 부족하면 아이 눈높이 문구로 안내한다', async () => {
    jest
      .mocked(investmentApi.buy)
      .mockRejectedValue({ response: { data: { code: 'INSUFFICIENT_POINT' } } });
    await renderWithQuery(<TradeButtons detail={detail} />);

    fireEvent.press(screen.getByText('사기'));
    pressAlertButton('살래요');

    await waitFor(() =>
      expect(jest.mocked(Alert.alert)).toHaveBeenCalledWith(
        '살 수 없어요',
        expect.stringContaining('도토리가 부족해요'),
      ),
    );
  });

  it('보유 종목은 팔기 확인 후 매도 API 를 호출한다', async () => {
    jest.mocked(investmentApi.sell).mockResolvedValue({
      ...order,
      transactionType: 'SELL',
      pointBalanceAfter: 15000,
      buyPrice: 10000,
      realizedProfit: 250,
    });
    await renderWithQuery(<TradeButtons detail={{ ...detail, holding: true }} />);

    fireEvent.press(screen.getByText('팔기'));
    pressAlertButton('팔래요');

    await waitFor(() => expect(jest.mocked(investmentApi.sell)).toHaveBeenCalledWith(1));
    // 결과는 번/잃은 도토리 한 줄만 말한다 (설명이 길다는 피드백으로 줄였다)
    await waitFor(() =>
      expect(jest.mocked(Alert.alert)).toHaveBeenCalledWith('팔았어요!', '250 도토리를 벌었어요!'),
    );
  });

  it('산 값보다 싸게 팔면 잃은 도토리만 말한다', async () => {
    jest.mocked(investmentApi.sell).mockResolvedValue({
      ...order,
      transactionType: 'SELL',
      unitPrice: 9700,
      totalAmount: 9700,
      buyPrice: 10000,
      realizedProfit: -300,
    });
    await renderWithQuery(<TradeButtons detail={{ ...detail, holding: true }} />);

    fireEvent.press(screen.getByText('팔기'));
    pressAlertButton('팔래요');

    await waitFor(() =>
      expect(jest.mocked(Alert.alert)).toHaveBeenCalledWith('팔았어요!', '300 도토리를 잃었어요.'),
    );
  });
});
