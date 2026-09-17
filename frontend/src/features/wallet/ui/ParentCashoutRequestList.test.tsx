import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { walletApi } from '@/features/wallet/api/walletApi';
import type { CashoutRequest } from '@/features/wallet/model/types';

import { ParentCashoutRequestList } from './ParentCashoutRequestList';

jest.mock('@/features/wallet/api/walletApi', () => ({
  walletApi: {
    getCashoutRequests: jest.fn(),
    approveCashout: jest.fn(),
    declineCashout: jest.fn(),
  },
}));

const mockApi = walletApi as jest.Mocked<typeof walletApi>;

const CHILDREN = [{ childId: 7, name: '민준' }];

const pendingRequest: CashoutRequest = {
  cashoutRequestId: 11,
  childId: 7,
  parentId: 2,
  pointAmount: 5_000,
  status: 'PENDING',
  requestedAt: '2026-09-15T18:23:00+09:00',
  processedAt: null,
};

const doneRequest: CashoutRequest = {
  cashoutRequestId: 9,
  childId: 7,
  parentId: 2,
  pointAmount: 1_000,
  status: 'SUCCEED',
  requestedAt: '2026-09-10T10:00:00+09:00',
  processedAt: '2026-09-10T11:00:00+09:00',
};

let queryClient: QueryClient;

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

// pendingRequest.parentId 와 같은 값 - 출금 계좌가 "내 계좌" 로 표시되는 쪽
const VIEWER_ID = 2;

function renderList(viewerId: number | undefined = VIEWER_ID) {
  return render(
    <ParentCashoutRequestList familyId={1} childList={CHILDREN} viewerId={viewerId} />,
    { wrapper },
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
});

// 캐시를 비우고 남은 타이머를 한 번 흘려보낸다. SectionList(VirtualizedList)가 셀 갱신을
// 50ms 타이머로 미뤄서, 그냥 끝내면 테스트 밖에서 깨어나 act 경고를 낸다.
// (가짜 타이머로 바꾸면 쿼리 resolve 가 안 풀려서 여기서는 진짜 타이머를 쓴다)
afterEach(async () => {
  queryClient.clear();
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 60));
  });
});

describe('ParentCashoutRequestList', () => {
  it('대기 중인 요청과 지난 내역을 나눠서 보여준다', async () => {
    mockApi.getCashoutRequests.mockResolvedValue([pendingRequest, doneRequest]);

    await renderList();

    await waitFor(() => expect(screen.getByText('승인 대기')).toBeTruthy());
    expect(screen.getByText('지난 내역')).toBeTruthy();
    // 포인트와 원을 함께 적는다 - 부모 계좌에서 빠지는 것은 원이다
    expect(screen.getByText('50,000원')).toBeTruthy();
    expect(screen.getByText('5,000P · 9월 15일 오후 6:23')).toBeTruthy();
    expect(screen.getByText('송금 완료')).toBeTruthy();
  });

  it('요청이 하나도 없으면 빈 상태를 안내한다', async () => {
    mockApi.getCashoutRequests.mockResolvedValue([]);

    await renderList();

    await waitFor(() => expect(screen.getByText(/아직 환전 요청이 없어요/)).toBeTruthy());
  });

  it('대기 중인 요청을 누르면 상세에 금액과 주고받는 사람을 보여준다', async () => {
    mockApi.getCashoutRequests.mockResolvedValue([pendingRequest]);

    await renderList();
    await waitFor(() => expect(screen.getByText('50,000원')).toBeTruthy());

    fireEvent.press(screen.getByText('50,000원'));

    expect(
      await screen.findByText(/내 계좌에서 50,000원이 민준의 계좌로 바로 이체돼요/),
    ).toBeTruthy();
    expect(screen.getByText('보내는 사람')).toBeTruthy();
    expect(screen.getByText('나 (내 계좌)')).toBeTruthy();
    expect(screen.getByText('받는 사람')).toBeTruthy();
  });

  it('출금 계좌가 다른 보호자 것이면 "내 계좌" 라고 하지 않는다', async () => {
    mockApi.getCashoutRequests.mockResolvedValue([pendingRequest]);

    // 가족에 보호자가 둘이면 서버가 첫 번째 부모를 출금자로 잡는다 - 승인하는 사람과 다를 수 있다
    await renderList(99);
    await waitFor(() => expect(screen.getByText('50,000원')).toBeTruthy());

    fireEvent.press(screen.getByText('50,000원'));

    expect(await screen.findByText('보호자 계좌')).toBeTruthy();
    expect(screen.queryByText('나 (내 계좌)')).toBeNull();
  });

  it('이미 끝난 요청은 결과만 보여주고 승인 버튼을 안 준다', async () => {
    mockApi.getCashoutRequests.mockResolvedValue([doneRequest]);

    await renderList();
    await waitFor(() => expect(screen.getByText('10,000원')).toBeTruthy());

    fireEvent.press(screen.getByText('10,000원'));

    expect(await screen.findByText('아이 계좌로 송금이 끝났어요.')).toBeTruthy();
    expect(screen.getByText('처리 시각')).toBeTruthy();
    expect(screen.queryByText('승인하기')).toBeNull();
  });

  it('승인하면 그 요청 id 로 승인 API 를 부른다', async () => {
    mockApi.getCashoutRequests.mockResolvedValue([pendingRequest]);
    mockApi.approveCashout.mockResolvedValue({ ...pendingRequest, status: 'SUCCEED' });

    await renderList();
    await waitFor(() => expect(screen.getByText('50,000원')).toBeTruthy());
    fireEvent.press(screen.getByText('50,000원'));

    fireEvent.press(await screen.findByText('승인하기'));

    await waitFor(() =>
      expect(mockApi.approveCashout).toHaveBeenCalledWith(1, 7, 11, expect.any(String)),
    );
    // 성공하면 모달이 닫히고 목록을 다시 받아온다 - 여기까지 기다려야 처리가 끝난 것이다
    await waitFor(() => expect(screen.queryByText('승인하기')).toBeNull());
    await waitFor(() => expect(mockApi.getCashoutRequests).toHaveBeenCalledTimes(2));
  });

  it('승인이 실패하면 실패 이유를 모달에 보여준다', async () => {
    mockApi.getCashoutRequests.mockResolvedValue([pendingRequest]);
    mockApi.approveCashout.mockRejectedValue({
      response: { data: { code: 'INSUFFICIENT_ACCOUNT_BALANCE' } },
    });

    await renderList();
    await waitFor(() => expect(screen.getByText('50,000원')).toBeTruthy());
    fireEvent.press(screen.getByText('50,000원'));
    fireEvent.press(await screen.findByText('승인하기'));

    await waitFor(() => expect(screen.getByText('계좌 잔액이 부족해요.')).toBeTruthy());
    // 실패해도 목록은 다시 받아온다 (서버가 환급하고 끝냈을 수 있다)
    await waitFor(() => expect(mockApi.getCashoutRequests).toHaveBeenCalledTimes(2));
  });
});
