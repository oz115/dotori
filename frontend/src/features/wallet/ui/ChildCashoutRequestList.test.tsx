import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, screen, waitFor } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { walletApi } from '@/features/wallet/api/walletApi';
import type { CashoutRequest } from '@/features/wallet/model/types';

import { ChildCashoutRequestList } from './ChildCashoutRequestList';

jest.mock('@/features/wallet/api/walletApi', () => ({
  walletApi: { getCashoutRequests: jest.fn() },
}));

const mockApi = walletApi as jest.Mocked<typeof walletApi>;

const pendingRequest: CashoutRequest = {
  cashoutRequestId: 11,
  childId: 7,
  parentId: 2,
  pointAmount: 5_000,
  status: 'PENDING',
  requestedAt: '2026-09-15T18:23:00+09:00',
  processedAt: null,
};

const declinedRequest: CashoutRequest = {
  cashoutRequestId: 9,
  childId: 7,
  parentId: 2,
  pointAmount: 1_000,
  status: 'DECLINED',
  requestedAt: '2026-09-10T10:00:00+09:00',
  processedAt: '2026-09-10T11:00:00+09:00',
};

let queryClient: QueryClient;

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function renderList() {
  return render(<ChildCashoutRequestList familyId={1} childId={7} />, { wrapper });
}

beforeEach(() => {
  jest.clearAllMocks();
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
});

// 목록과 같은 이유로 남은 타이머를 흘려보낸다 (ParentCashoutRequestList.test 참고)
afterEach(async () => {
  queryClient.clear();
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 60));
  });
});

describe('ChildCashoutRequestList', () => {
  it('기다리는 중과 지난 내역을 나눠서 보여준다', async () => {
    mockApi.getCashoutRequests.mockResolvedValue([pendingRequest, declinedRequest]);

    await renderList();

    await waitFor(() => expect(screen.getByText('기다리는 중')).toBeTruthy());
    expect(screen.getByText('지난 내역')).toBeTruthy();
    expect(screen.getByText('50,000원')).toBeTruthy();
    expect(screen.getByText('거절')).toBeTruthy();
  });

  it('상태를 서버에 따로 묻지 않고 전체를 한 번에 받는다', async () => {
    mockApi.getCashoutRequests.mockResolvedValue([pendingRequest]);

    await renderList();

    await waitFor(() => expect(mockApi.getCashoutRequests).toHaveBeenCalledWith(1, 7, undefined));
  });

  it('요청한 적이 없으면 빈 상태를 안내한다', async () => {
    mockApi.getCashoutRequests.mockResolvedValue([]);

    await renderList();

    await waitFor(() => expect(screen.getByText(/아직 환전을 신청한 적이 없어요/)).toBeTruthy());
  });
});
