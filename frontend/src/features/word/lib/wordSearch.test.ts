import { groupByChosung } from '@/features/word/lib/wordSearch';
import type { WordSummary } from '@/features/word/model/types';

const WORDS: WordSummary[] = [
  {
    wordId: 1,
    word: '적금',
    difficultyLevel: 1,
    category: 'SAVINGS',
  },
  {
    wordId: 2,
    word: '주식',
    difficultyLevel: 3,
    category: 'INVESTMENT',
  },
  {
    wordId: 3,
    word: '환율',
    difficultyLevel: 2,
    category: 'ECONOMY',
  },
];

describe('groupByChosung', () => {
  it('초성 순서대로 그룹을 만든다', () => {
    const groups = groupByChosung(WORDS);
    expect(groups.map((g) => g.chosung)).toEqual(['ㅈ', 'ㅎ']);
    expect(groups.find((g) => g.chosung === 'ㅈ')?.words.map((w) => w.word)).toEqual([
      '적금',
      '주식',
    ]);
  });

  it('결과가 없는 초성은 그룹을 만들지 않는다', () => {
    const groups = groupByChosung<WordSummary>([]);
    expect(groups).toEqual([]);
  });
});
