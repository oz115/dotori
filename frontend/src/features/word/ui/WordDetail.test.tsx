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
    easyDefinition: '적금은 매달 조금씩 돈을 모으는 거예요.',
    difficultyLevel: 1,
    category: 'SAVINGS',
  },
  2: {
    wordId: 2,
    word: '가계부',
    definition: '수입·지출 기록장',
    // 용어사전에 없던 단어 — 아직 쉬운 설명이 비어 있다
    easyDefinition: null,
    difficultyLevel: 1,
    category: 'SAVINGS',
  },
};

async function show(props: {
  wordId: number;
  category?: 'SAVINGS';
  difficulty?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={client}>
      <WordDetail
        wordId={props.wordId}
        category={props.category}
        difficulty={props.difficulty}
        onBack={jest.fn()}
      />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  jest.mocked(wordApi.getWord).mockImplementation((wordId) => Promise.resolve(DETAILS[wordId]));
  jest.mocked(wordApi.getWords).mockResolvedValue({
    words: Object.values(DETAILS).map(
      ({ definition: _d, easyDefinition: _e, ...summary }) => summary,
    ),
    page: 0,
    totalPages: 1,
  });
});

it('다음 단어는 목록에 보이는 가나다 순서를 따른다 — db 저장 순서가 아니다', async () => {
  await show({ wordId: 2 }); // 가계부 (가나다 첫 번째, id 는 마지막)

  // 버튼에 글자가 없다 — 화살표만 있고 라벨은 스크린리더용이다
  fireEvent.press(await screen.findByLabelText('다음 단어'));

  expect(await screen.findByText('적금')).toBeTruthy();
});

it('카테고리를 넘겨받으면 그 카테고리 목록을 기준으로 이웃을 찾는다', async () => {
  await show({ wordId: 2, category: 'SAVINGS' });
  await screen.findByText('가계부');

  expect(wordApi.getWords).toHaveBeenCalledWith(expect.objectContaining({ category: 'SAVINGS' }));
});

it('쉬운 설명이 있으면 정석 뜻과 함께 보여준다', async () => {
  await show({ wordId: 1 });

  // 정석 뜻을 대체하지 않고 덧붙인다 — 어려운 말도 눈에 익혀야 하는 단어장이다
  expect(await screen.findByText('돈을 모으는 방법')).toBeTruthy();
  expect(screen.getByText('쉽게 말하면')).toBeTruthy();
  expect(screen.getByText('적금은 매달 조금씩 돈을 모으는 거예요.')).toBeTruthy();
});

it('쉬운 설명이 아직 없는 단어는 그 블록을 빼고 뜻만 보여준다', async () => {
  await show({ wordId: 2 });

  expect(await screen.findByText('수입·지출 기록장')).toBeTruthy();
  expect(screen.queryByText('쉽게 말하면')).toBeNull();
});

it('이웃이 없으면 이전/다음 버튼이 눌리지 않는다', async () => {
  // 검색 결과에서 바로 들어오면 목록 순서를 몰라서 이동할 곳이 없다
  jest.mocked(wordApi.getWords).mockResolvedValue({ words: [], page: 0, totalPages: 1 });
  await show({ wordId: 1 });
  await screen.findByText('적금');

  expect(screen.getByLabelText('이전 단어').props.accessibilityState?.disabled).toBe(true);
  expect(screen.getByLabelText('다음 단어').props.accessibilityState?.disabled).toBe(true);
});

describe('이전/다음 범위', () => {
  const THREE = {
    words: [
      { wordId: 1, word: '가격', difficultyLevel: 1, category: 'SAVINGS' as const },
      { wordId: 2, word: '금리', difficultyLevel: 3, category: 'SAVINGS' as const },
      { wordId: 3, word: '나눔', difficultyLevel: 1, category: 'SAVINGS' as const },
    ],
    page: 0,
    totalPages: 1,
  };

  beforeEach(() => {
    jest.mocked(wordApi.getWords).mockResolvedValue(THREE);
    jest.mocked(wordApi.getWord).mockImplementation((wordId) =>
      Promise.resolve({
        ...THREE.words.find((w) => w.wordId === wordId)!,
        definition: '뜻',
        easyDefinition: null,
      }),
    );
  });

  it('난이도 필터를 넘겨받으면 그 범위 안에서만 이동한다', async () => {
    // 목록에서 초급(가격·나눔)만 보다 들어왔다 — 중급인 금리를 건너뛰어야 한다
    await show({ wordId: 1, category: 'SAVINGS', difficulty: 'BEGINNER' });
    await screen.findByText('가격');

    fireEvent.press(screen.getByLabelText('다음 단어'));

    expect(await screen.findByText('나눔')).toBeTruthy();
  });

  it('필터가 없으면 그 카테고리 전체를 돈다', async () => {
    await show({ wordId: 1, category: 'SAVINGS' });
    await screen.findByText('가격');

    fireEvent.press(screen.getByLabelText('다음 단어'));

    expect(await screen.findByText('금리')).toBeTruthy();
  });
});
