import { fireEvent, render, screen } from '@testing-library/react-native';

import type { InvestmentNews } from '@/features/investment/model/types';

import { NewsDetailModal } from './NewsDetailModal';

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

describe('NewsDetailModal', () => {
  it('news 가 null 이면 아무것도 그리지 않는다', async () => {
    await render(<NewsDetailModal news={null} onClose={jest.fn()} />);

    expect(screen.queryByText('확인')).toBeNull();
  });

  it('뉴스 본문·관련 회사·영향 방향을 보여준다', async () => {
    await render(<NewsDetailModal news={news} onClose={jest.fn()} />);

    expect(screen.getByText(content)).toBeTruthy();
    expect(screen.getByText('태수 마트')).toBeTruthy();
    expect(screen.getByText('📉 내리는 뉴스')).toBeTruthy();
  });

  it('오르는 뉴스는 상승 배지를 보여준다', async () => {
    await render(<NewsDetailModal news={{ ...news, impact: 'POSITIVE' }} onClose={jest.fn()} />);

    expect(screen.getByText('📈 오르는 뉴스')).toBeTruthy();
  });

  it('확인 버튼을 누르면 onClose 가 호출된다', async () => {
    const onClose = jest.fn();
    await render(<NewsDetailModal news={news} onClose={onClose} />);

    fireEvent.press(screen.getByText('확인'));

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
