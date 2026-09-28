import {
  difficultyBadgeColors,
  difficultyLabel,
  difficultyTier,
  difficultyTierColors,
  TIER_ORDER,
} from '@/features/word/lib/difficultyTier';
import { theme } from '@/shared/theme';

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

describe('difficultyTierColors', () => {
  it('세 구간이 서로 다른 색을 쓴다 — 같아지면 배지·칩·목록 아이콘의 신호가 사라진다', () => {
    const backgrounds = TIER_ORDER.map((tier) => difficultyTierColors(theme, tier).bg);

    expect(new Set(backgrounds).size).toBe(TIER_ORDER.length);
  });

  it('배지 색은 구간 색과 같다 — level(1~4)을 구간으로 접어 같은 곳에서 고른다', () => {
    expect(difficultyBadgeColors(theme, 2)).toMatchObject(
      difficultyTierColors(theme, 'INTERMEDIATE'),
    );
  });
});
