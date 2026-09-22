import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { walletApi } from '@/features/wallet/api/walletApi';
import type { CashoutRequest, PointTransactionItem } from '@/features/wallet/model/types';

import { ChildWalletHistory } from './ChildWalletHistory';

jest.mock('@/features/wallet/api/walletApi', () => ({
  walletApi: { getTransactions: jest.fn(), getCashoutRequests: jest.fn() },
}));

const mockApi = walletApi as jest.Mocked<typeof walletApi>;

const grant: PointTransactionItem = {
  pointTransactionId: 1,
  transactionType: 'GRANT',
  transactionDirection: 'IN',
  amount: 1_000,
  balanceAfter: 6_000,
  description: '용돈 지급',
  createdAt: '2026-09-21T15:00:00+09:00',
};

const purchase: PointTransactionItem = {
  pointTransactionId: 2,
  transactionType: 'PURCHASE',
  transactionDirection: 'OUT',
  amount: 500,
  balanceAfter: 5_500,
  description: null,
  createdAt: '2026-09-20T10:00:00+09:00',
};

const cashoutRequest: CashoutRequest = {
  cashoutRequestId: 11,
  childId: 7,
  parentId: 2,
  pointAmount: 5_000,
  status: 'PENDING',
  requestedAt: '2026-09-15T18:23:00+09:00',
  processedAt: null,
};

let queryClient: QueryClient;

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function renderHistory() {
  return render(<ChildWalletHistory familyId={1} childId={7} />, { wrapper });
}

beforeEach(() => {
  jest.clearAllMocks();
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  mockApi.getTransactions.mockResolvedValue({
    childId: 7,
    balance: 6_000,
    page: { items: [grant, purchase], nextCursor: null, hasNext: false },
  });
  mockApi.getCashoutRequests.mockResolvedValue([cashoutRequest]);
});

// 목록과 같은 이유로 남은 타이머를 흘려보낸다 (ChildCashoutRequestList.test 참고)
afterEach(async () => {
  queryClient.clear();
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 60));
  });
});

describe('ChildWalletHistory', () => {
  it('도토리 내역 탭이 기본이고 입금·출금을 방향 색 없이도 부호로 구분해 보여준다', async () => {
    await renderHistory();

    await waitFor(() => expect(screen.getByText('용돈 지급')).toBeTruthy());
    expect(screen.getByText('+1,000P')).toBeTruthy();
    // description 이 없는 거래는 사유 라벨이 제목이 된다
    expect(screen.getByText('상점 구매')).toBeTruthy();
    expect(screen.getByText('-500P')).toBeTruthy();
    expect(screen.getByText('잔액 6,000P')).toBeTruthy();
  });

  it('출금만 필터를 고르면 들어온 도토리는 목록에서 빠진다', async () => {
    await renderHistory();
    await waitFor(() => expect(screen.getByText('용돈 지급')).toBeTruthy());

    await fireEvent.press(screen.getByText('출금만'));

    expect(screen.queryByText('용돈 지급')).toBeNull();
    expect(screen.getByText('-500P')).toBeTruthy();
  });

  it('환전 내역 탭으로 넘어가면 환전 요청 목록을 보여준다', async () => {
    await renderHistory();
    await waitFor(() => expect(screen.getByText('용돈 지급')).toBeTruthy());

    await fireEvent.press(screen.getByText('환전 내역'));

    await waitFor(() => expect(screen.getByText('기다리는 중')).toBeTruthy());
    expect(screen.getByText('50,000원')).toBeTruthy();
  });
});
