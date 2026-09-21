import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react-native';

import { wordApi } from '@/features/word/api/wordApi';
import { WordCatalog } from './WordCatalog';

jest.mock('@/features/word/api/wordApi', () => ({
  wordApi: { getWords: jest.fn(), searchWords: jest.fn(), getWord: jest.fn() },
}));

const onSelectWord = jest.fn();

async function show() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={client}>
      <WordCatalog onSelectWord={onSelectWord} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  jest.clearAllMocks();
});

it('단어를 누르면 화면이 넘겨 준 이동 함수에 wordId 를 전달한다 — 부모·아이가 같은 화면을 쓴다', async () => {
  jest.mocked(wordApi.getWords).mockResolvedValue({
    words: [
      { wordId: 5, word: '중앙은행', difficultyLevel: 2, category: 'BANKING' },
      { wordId: 9, word: '인플레이션', difficultyLevel: 3, category: 'ECONOMY' },
    ],
    page: 0,
    totalPages: 1,
  });
  await show();

  fireEvent.press(await screen.findByText('중앙은행'));

  expect(onSelectWord).toHaveBeenCalledWith(5);
});

it('단어를 불러오지 못하면 오류 문구를 보여 준다', async () => {
  jest.mocked(wordApi.getWords).mockRejectedValue({ response: { status: 500, data: {} } });
  await show();

  expect(await screen.findByText('단어를 불러오지 못했어요.')).toBeTruthy();
  expect(onSelectWord).not.toHaveBeenCalled();
});
