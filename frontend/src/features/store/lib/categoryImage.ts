import type { StoreProductCategory } from '@/features/store/model/types';

/**
 * 카테고리 대표 이미지. 상품 개별 이미지는 아직 없어 카테고리 단위로 재사용한다.
 * 지금은 색상만 채운 placeholder이니, 실제 일러스트가 assets/images/store/ 에 같은
 * 파일명(game.png 등)으로 들어오면 이 매핑은 코드 변경 없이 자동으로 교체된다.
 */
const CATEGORY_IMAGES: Record<StoreProductCategory, number> = {
  GAME: require('@/assets/images/store/game.png'),
  SNACK: require('@/assets/images/store/snack.png'),
  TOY: require('@/assets/images/store/toy.png'),
  OUTING: require('@/assets/images/store/outing.png'),
  PRIVILEGE: require('@/assets/images/store/privilege.png'),
  ETC: require('@/assets/images/store/etc.png'),
};

export function categoryImage(category: StoreProductCategory) {
  return CATEGORY_IMAGES[category];
}
