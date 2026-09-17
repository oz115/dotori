import type { Theme } from '@/shared/theme';

export type DifficultyTier = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

const TIER_LABELS: Record<DifficultyTier, string> = {
  BEGINNER: '초급',
  INTERMEDIATE: '중급',
  ADVANCED: '고급',
};

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

/** 난이도 배지 색 — 시맨틱 톤온톤(success/warning/danger)으로 "쉬움→위험"이 읽히게 한다. */
export function difficultyBadgeColors(theme: Theme, level: number): { bg: string; text: string } {
  switch (difficultyTier(level)) {
    case 'BEGINNER':
      return { bg: theme.color.status.success.subtle, text: theme.color.status.success.text };
    case 'INTERMEDIATE':
      return { bg: theme.color.status.warning.subtle, text: theme.color.status.warning.text };
    case 'ADVANCED':
      return { bg: theme.color.status.danger.subtle, text: theme.color.status.danger.text };
  }
}
