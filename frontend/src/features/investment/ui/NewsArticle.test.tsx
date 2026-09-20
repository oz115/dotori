import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react-native';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { NewsArticle } from './NewsArticle';

jest.mock('@/features/investment/api/investmentApi', () => ({
  investmentApi: { getNewsDetail: jest.fn() },
}));
const news = {
  investmentNewsId: 1,
  title: '밀 생산량 감소',
  content: '날씨 영향으로 밀 수확량이 줄었어요.',
  impact: 'NEGATIVE' as const,
  relatedProducts: [
    { investmentProductId: 8, productName: '고소한빵', category: 'STOCK' as const },
  ],
  publishedAt: '2026-09-19T00:05:00+09:00',
};
async function show() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={client}>
      <NewsArticle newsId={1} />
    </QueryClientProvider>,
  );
}

it('뉴스 본문을 표시하지만 관련 회사와 등락 방향은 표시하지 않는다', async () => {
  jest.mocked(investmentApi.getNewsDetail).mockResolvedValue(news);
  await show();
  expect(await screen.findByText(news.content)).toBeTruthy();
  expect(screen.queryByText('고소한빵')).toBeNull();
  expect(screen.queryByText('NEGATIVE')).toBeNull();
});

it('실패 후 다시 불러오면 새 응답을 보여준다', async () => {
  jest
    .mocked(investmentApi.getNewsDetail)
    .mockRejectedValueOnce(new Error('offline'))
    .mockResolvedValue(news);
  await show();
  fireEvent.press(await screen.findByText('다시 불러오기'));
  expect(await screen.findByText(news.content)).toBeTruthy();
});
