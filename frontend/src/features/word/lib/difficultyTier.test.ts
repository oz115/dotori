import { difficultyLabel, difficultyTier } from '@/features/word/lib/difficultyTier';

describe('difficultyTier', () => {
  it('1은 초급이다', () => {
    expect(difficultyTier(1)).toBe('BEGINNER');
  });

  it('2~3은 중급이다', () => {
    expect(difficultyTier(2)).toBe('INTERMEDIATE');
    expect(difficultyTier(3)).toBe('INTERMEDIATE');
  });

  it('4는 고급이다', () => {
    expect(difficultyTier(4)).toBe('ADVANCED');
  });

  it('범위를 벗어나면 가장 가까운 구간으로 묶는다', () => {
    expect(difficultyTier(0)).toBe('BEGINNER');
    expect(difficultyTier(5)).toBe('ADVANCED');
  });
});

describe('difficultyLabel', () => {
  it('구간에 맞는 한글 라벨을 돌려준다', () => {
    expect(difficultyLabel(1)).toBe('초급');
    expect(difficultyLabel(3)).toBe('중급');
    expect(difficultyLabel(4)).toBe('고급');
  });
});
