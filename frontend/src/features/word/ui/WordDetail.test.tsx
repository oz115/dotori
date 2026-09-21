import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react-native';

import { wordApi } from '@/features/word/api/wordApi';
import type { WordDetailInfo } from '@/features/word/model/types';
import { WordDetail } from './WordDetail';

jest.mock('@/features/word/api/wordApi', () => ({
  wordApi: { getWords: jest.fn(), searchWords: jest.fn(), getWord: jest.fn() },
}));

// db 저장(id) 순서와 가나다 순서가 다르게 배치 — 이동 순서가 어느 쪽을 따르는지 드러난다
const DETAILS: Record<number, WordDetailInfo> = {
  1: {
    wordId: 1,
    word: '적금',
    definition: '돈을 모으는 방법',
    difficultyLevel: 1,
    category: 'SAVINGS',
  },
  2: {
    wordId: 2,
    word: '가계부',
    definition: '수입·지출 기록장',
    difficultyLevel: 1,
    category: 'SAVINGS',
  },
};

async function show(props: { wordId: number; category?: 'SAVINGS' }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={client}>
      <WordDetail wordId={props.wordId} category={props.category} onBack={jest.fn()} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  jest.mocked(wordApi.getWord).mockImplementation((wordId) => Promise.resolve(DETAILS[wordId]));
  jest.mocked(wordApi.getWords).mockResolvedValue({
    words: Object.values(DETAILS).map(({ definition: _, ...summary }) => summary),
    page: 0,
    totalPages: 1,
  });
});

it('다음 단어는 목록에 보이는 가나다 순서를 따른다 — db 저장 순서가 아니다', async () => {
  await show({ wordId: 2 }); // 가계부 (가나다 첫 번째, id 는 마지막)

  fireEvent.press(await screen.findByText('다음 단어 →'));

  expect(await screen.findByText('적금')).toBeTruthy();
});

it('카테고리를 넘겨받으면 그 카테고리 목록을 기준으로 이웃을 찾는다', async () => {
  await show({ wordId: 2, category: 'SAVINGS' });
  await screen.findByText('가계부');

  expect(wordApi.getWords).toHaveBeenCalledWith(expect.objectContaining({ category: 'SAVINGS' }));
});
