import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { httpClient } from '@/shared/api';
import { useChildAssets } from './useChildAssets';

// 다른 슬라이스의 api 파일이 아니라 공용 HTTP 클라이언트를 막는다 — 위젯은 슬라이스 내부를 모른다
jest.mock('@/shared/api', () => ({
  ...jest.requireActual('@/shared/api'),
  httpClient: { get: jest.fn() },
}));

const holding = (id: number, name: string, buyPrice: number, currentPrice: number) => ({
  investmentProductId: id,
  productName: name,
  quantity: 1,
  buyPrice,
  currentPrice,
});

const summary = (overrides: Partial<Record<string, number | null>> = {}) => ({
  activeCount: 0,
  maturedCount: 0,
  totalCurrentAmount: 0,
  totalAccruedInterest: 0,
  totalAmount: 0,
  nearestMaturityAt: null,
  ...overrides,
});

type Responses = { savings: () => Promise<unknown>; holdings: () => Promise<unknown> };

/** 요청 경로로 예적금 요약 / 투자 보유 응답을 갈라 준다 */
function givenServer({ savings, holdings }: Responses) {
  jest
    .mocked(httpClient.get)
    .mockImplementation(((url: string) =>
      url.endsWith('/savings/summary') ? savings() : holdings()) as never);
}

const ok = (data: unknown) => () => Promise.resolve({ data });
const fail = () => () => Promise.reject({ response: { status: 500, data: {} } });

function wrapper() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  };
}

beforeEach(() => {
  jest.clearAllMocks();
});

it('예적금은 서버 요약의 건수·원금+이자, 투자는 현재 시세 기준 보유 가치를 보여 준다', async () => {
  givenServer({
    savings: ok(
      summary({
        activeCount: 2,
        maturedCount: 1,
        totalCurrentAmount: 4000,
        totalAccruedInterest: 300,
        totalAmount: 4300,
      }),
    ),
    holdings: ok({
      holdings: [holding(1, '고소한빵', 1000, 1200), holding(2, '튼튼건설', 800, 700)],
    }),
  });

  const { result } = await renderHook(() => useChildAssets(10, 2), { wrapper: wrapper() });

  await waitFor(() => expect(result.current.savings.subtitle).toBe('3개 상품에 저축 중'));
  expect(result.current.savings.amount).toBe('4,300P');
  await waitFor(() => expect(result.current.investment.subtitle).toBe('2개 회사 보유 중'));
  // 산 값(1000 + 800 = 1800)이 아니라 지금 시세(1200 + 700). 투자 탭 "보유 자산"은
  // 산 값 1800 + 평가손익 100 = 1900 이라 같은 값이다.
  expect(result.current.investment.amount).toBe('1,900P');

  // 본인 자녀 id 로 조회한다 — 캐시 키·요청에 사용자가 들어간다
  expect(httpClient.get).toHaveBeenCalledWith('/api/v1/families/10/children/2/savings/summary');
  expect(httpClient.get).toHaveBeenCalledWith('/api/v1/investments/holdings', {
    params: { childId: 2 },
  });
});

it('가입·보유가 없으면 0P 와 빈 상태 문구를 보여 준다', async () => {
  givenServer({ savings: ok(summary()), holdings: ok({ holdings: [] }) });

  const { result } = await renderHook(() => useChildAssets(10, 2), { wrapper: wrapper() });

  await waitFor(() =>
    expect(result.current.savings).toEqual({ subtitle: '가입한 상품 없음', amount: '0P' }),
  );
  await waitFor(() =>
    expect(result.current.investment).toEqual({ subtitle: '보유 종목 없음', amount: '0P' }),
  );
});

it('한쪽이 실패해도 다른 쪽 값은 그대로 보여 준다', async () => {
  givenServer({
    savings: fail(),
    holdings: ok({ holdings: [holding(1, '고소한빵', 1000, 1200)] }),
  });

  const { result } = await renderHook(() => useChildAssets(10, 2), { wrapper: wrapper() });

  await waitFor(() =>
    expect(result.current.savings).toEqual({ subtitle: '불러오지 못했어요', amount: '-' }),
  );
  await waitFor(() =>
    expect(result.current.investment).toEqual({ subtitle: '1개 회사 보유 중', amount: '1,200P' }),
  );
});

it('가족·자녀 id 가 준비되기 전에는 조회하지 않고 불러오는 중으로 둔다', async () => {
  const { result } = await renderHook(() => useChildAssets(undefined, undefined), {
    wrapper: wrapper(),
  });

  expect(result.current.savings.subtitle).toBe('불러오는 중이에요');
  expect(result.current.investment.subtitle).toBe('불러오는 중이에요');
  expect(httpClient.get).not.toHaveBeenCalled();
});
