import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react-native';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { NewsReview } from './NewsReview';

jest.mock('@/features/investment/api/investmentApi', () => ({
  investmentApi: { getNewsReview: jest.fn() },
}));

async function show() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={client}>
      <NewsReview />
    </QueryClientProvider>,
  );
}

beforeEach(() => jest.clearAllMocks());

it('대기 중에는 응답에 회사가 있어도 정답을 화면에 표시하지 않는다', async () => {
  jest.mocked(investmentApi.getNewsReview).mockResolvedValue({
    newsDate: '2026-09-18',
    reviews: [
      {
        investmentNewsId: 1,
        title: '밀 가격 상승',
        content: '수확량이 줄었어요.',
        ready: false,
        productName: '고소한빵',
        previousPrice: 1000,
        currentPrice: 900,
        explanation: '재료비 증가',
      },
    ],
  });
  await show();
  expect(await screen.findByText('오늘 가격에 반영되면 결과를 보여줄게요.')).toBeTruthy();
  expect(screen.queryByText('고소한빵')).toBeNull();
  expect(screen.queryByText('재료비 증가')).toBeNull();
});

it('반영 후 실제 가격과 이유를 표시한다', async () => {
  jest.mocked(investmentApi.getNewsReview).mockResolvedValue({
    newsDate: '2026-09-18',
    reviews: [
      {
        investmentNewsId: 1,
        title: '밀 가격 상승',
        content: '수확량이 줄었어요.',
        ready: true,
        productName: '고소한빵',
        previousPrice: 1000,
        currentPrice: 900,
        explanation: '재료비가 늘어 가격이 내렸어요.',
      },
    ],
  });
  await show();
  // 가격 변화 박스와 해설 문단을 나눠 보여준다 (가독성 피드백)
  expect(await screen.findByText('고소한빵 가격 변화')).toBeTruthy();
  expect(screen.getByText(/1,000 → 900 도토리/)).toBeTruthy();
  expect(screen.getByText('왜 그랬을까요?')).toBeTruthy();
  expect(screen.getByText('재료비가 늘어 가격이 내렸어요.')).toBeTruthy();
});

it('조회 실패 후 다시 불러올 수 있다', async () => {
  jest
    .mocked(investmentApi.getNewsReview)
    .mockRejectedValueOnce(new Error('offline'))
    .mockResolvedValue({ newsDate: '2026-09-18', reviews: [] });
  await show();
  fireEvent.press(await screen.findByText('다시 불러오기'));
  expect(await screen.findByText('아직 돌아볼 어제 뉴스가 없어요.')).toBeTruthy();
});

it('미지원 서버에서는 불가능한 다시 불러오기 버튼을 노출하지 않는다', async () => {
  jest.mocked(investmentApi.getNewsReview).mockResolvedValue(null);
  await show();
  expect(await screen.findByText('지난 뉴스 해설은 아직 이용할 수 없어요.')).toBeTruthy();
  expect(screen.queryByText('다시 불러오기')).toBeNull();
});
