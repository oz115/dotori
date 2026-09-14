import type { Theme } from '@/shared/theme';
import type { StoreProductCategory } from '@/features/store/model/types';

export type CategoryBadgeColors = {
  bg: string;
  text: string;
};

/**
 * 카테고리별 배지 색. 기존 semantic 색을 그대로 재사용하고(GAME=info, SNACK=danger,
 * OUTING=brand, PRIVILEGE=warning), TOY·ETC 만 palette 원본에서 옅은 배경을 계산해 쓴다 —
 * 둘 다 subtle/text 쌍이 없는 색(acorn 브라운, neutral 회색)이라서다.
 */
export function categoryBadgeColors(
  theme: Theme,
  category: StoreProductCategory,
): CategoryBadgeColors {
  switch (category) {
    case 'GAME':
      return { bg: theme.color.status.info.subtle, text: theme.color.status.info.text };
    case 'SNACK':
      return { bg: theme.color.status.danger.subtle, text: theme.color.status.danger.text };
    case 'TOY':
      return { bg: 'rgba(130, 84, 48, 0.12)', text: theme.palette.acorn };
    case 'OUTING':
      return { bg: theme.color.brand.subtle, text: theme.color.brand.subtleText };
    case 'PRIVILEGE':
      return { bg: theme.color.status.warning.subtle, text: theme.color.status.warning.text };
    case 'ETC':
      return { bg: theme.color.background.surfaceSunken, text: theme.color.text.muted };
  }
}
