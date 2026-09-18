import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react-native';
import type { ReactElement } from 'react';

import { investmentApi } from '@/features/investment/api/investmentApi';
import type { InvestmentNews } from '@/features/investment/model/types';

import { NewsDetailModal } from './NewsDetailModal';

jest.mock('@/features/investment/api/investmentApi', () => ({
  investmentApi: {
    getNewsDetail: jest.fn(),
  },
}));

const content = '태수 마트 근처에 아주 큰 마트가 새로 문을 열었어요.';

const news: InvestmentNews = {
  id: 3,
  tag: '생활',
  title: '태수 마트,\n손님이 줄었어요',
  body: '근처에 큰 마트가 새로 생겨서 걱정이에요.',
  content,
  impact: 'NEGATIVE',
  relatedProductName: '태수 마트',
  date: '2025. 09. 07',
  emoji: '🛒',
};

/** useQuery 를 쓰는 컴포넌트라 매 테스트 새 QueryClient 로 감싼다 */
function renderWithQuery(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

describe('NewsDetailModal', () => {
  it('news 가 null 이면 아무것도 그리지 않는다', async () => {
    await renderWithQuery(<NewsDetailModal news={null} onClose={jest.fn()} />);

    expect(screen.queryByText('확인')).toBeNull();
  });

  it('뉴스 본문과 관련 회사를 보여주되, 오를지/내릴지 방향은 알려주지 않는다', async () => {
    await renderWithQuery(<NewsDetailModal news={news} onClose={jest.fn()} />);

    expect(screen.getByText(content)).toBeTruthy();
    expect(screen.getByText('태수 마트')).toBeTruthy();
    // 방향 배지는 뺐다 — 답을 미리 주면 아이가 스스로 판단할 게 없어진다
    expect(screen.queryByText('📉 내리는 뉴스')).toBeNull();
  });

  it('본문이 없으면 상세 API 로 채운다', async () => {
    jest.mocked(investmentApi.getNewsDetail).mockResolvedValue({
      investmentNewsId: 3,
      title: news.title,
      content,
      impact: 'NEGATIVE',
      relatedProducts: [],
      publishedAt: '2025-09-07T06:00:00+09:00',
    });

    await renderWithQuery(
      <NewsDetailModal news={{ ...news, content: undefined }} onClose={jest.fn()} />,
    );

    expect(await screen.findByText(content)).toBeTruthy();
    expect(jest.mocked(investmentApi.getNewsDetail)).toHaveBeenCalledWith(3);
  });

  it('확인 버튼을 누르면 onClose 가 호출된다', async () => {
    const onClose = jest.fn();
    await renderWithQuery(<NewsDetailModal news={news} onClose={onClose} />);

    fireEvent.press(screen.getByText('확인'));

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
