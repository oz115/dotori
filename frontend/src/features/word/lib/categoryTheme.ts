import type { Theme } from '@/shared/theme';

/**
 * 카테고리 배지 색. 상점(`features/store/lib/categoryTheme.ts`)과 달리 카테고리마다
 * 다른 색을 주지 않는다 — 색은 난이도(초급/중급/고급) 쪽에 몰아주고, 카테고리는 검정
 * 바탕 + 크림 글자로 어디서나 또렷하게 고정했다 (디자인 리뷰 D안).
 */
export function categoryBadgeColors(theme: Theme): { bg: string; text: string } {
  return { bg: theme.color.brand.black, text: theme.color.accent.cream };
}
