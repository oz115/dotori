import type { ComponentProps } from 'react';
import type { Ionicons } from '@expo/vector-icons';

import type { StoreProductCategory } from '@/features/store/model/types';

type IoniconName = ComponentProps<typeof Ionicons>['name'];

/**
 * 카테고리별 대표 아이콘. 상품엔 이미지가 없고(카테고리별 S3 이미지는 아직 없음)
 * 카테고리만 있어서, 목록·카드에서 이미지 자리 대신 쓴다.
 */
const CATEGORY_ICONS: Record<StoreProductCategory, IoniconName> = {
  GAME: 'game-controller-outline',
  SNACK: 'fast-food-outline',
  TOY: 'cube-outline',
  OUTING: 'bus-outline',
  PRIVILEGE: 'star-outline',
  ETC: 'ellipsis-horizontal-outline',
};

export function categoryIcon(category: StoreProductCategory): IoniconName {
  return CATEGORY_ICONS[category];
}
