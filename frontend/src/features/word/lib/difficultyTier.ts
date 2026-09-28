import type { Theme } from '@/shared/theme';

export type DifficultyTier = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

const TIER_LABELS: Record<DifficultyTier, string> = {
  BEGINNER: '초급',
  INTERMEDIATE: '중급',
  ADVANCED: '고급',
};

/** 필터 칩이 쉬운 것부터 늘어놓을 순서 */
export const TIER_ORDER: DifficultyTier[] = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

export function tierLabel(tier: DifficultyTier): string {
  return TIER_LABELS[tier];
}

/**
 * words.difficulty_level(1~4) → 난이도 구간. AI 서버 `app/domains/quiz/services.py`의
 * `TIER_TO_DIFFICULTY`(1→BEGINNER, 2~3→INTERMEDIATE, 4→ADVANCED)와 값을 맞췄다 — 두 곳이
 * 어긋나면 퀴즈 난이도와 단어장 난이도 표시가 서로 다른 기준을 가리키게 된다.
 */
export function difficultyTier(level: number): DifficultyTier {
  if (level <= 1) return 'BEGINNER';
  if (level >= 4) return 'ADVANCED';
  return 'INTERMEDIATE';
}

export function difficultyLabel(level: number): string {
  return TIER_LABELS[difficultyTier(level)];
}

/**
 * 난이도별 색 한 벌 — 시맨틱 톤온톤(success/warning/danger)으로 "쉬움→위험"이 읽히게 한다.
 * 상세의 배지와 목록의 필터 칩이 같은 색을 써야 아이가 둘을 같은 것으로 알아본다.
 */
export function difficultyTierColors(
  theme: Theme,
  tier: DifficultyTier,
): { bg: string; text: string; solid: string } {
  switch (tier) {
    case 'BEGINNER':
      return {
        bg: theme.color.status.success.subtle,
        text: theme.color.status.success.text,
        solid: theme.color.status.success.solid,
      };
    case 'INTERMEDIATE':
      return {
        bg: theme.color.status.warning.subtle,
        text: theme.color.status.warning.text,
        solid: theme.color.status.warning.solid,
      };
    case 'ADVANCED':
      return {
        bg: theme.color.status.danger.subtle,
        text: theme.color.status.danger.text,
        solid: theme.color.status.danger.solid,
      };
  }
}

/** 난이도 배지 색. `difficulty_level`(1~4)을 구간으로 접어 위 색을 고른다. */
export function difficultyBadgeColors(theme: Theme, level: number): { bg: string; text: string } {
  return difficultyTierColors(theme, difficultyTier(level));
}
