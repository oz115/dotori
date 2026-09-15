import type { StoreProductCategory } from '@/features/store/model/types';

/** 상품 상세 배너에 쓰는 카테고리별 캐치프레이즈. 상품마다가 아니라 카테고리 단위로 고정이다. */
const CATEGORY_TAGLINES: Record<StoreProductCategory, string> = {
  GAME: '신나게 즐기는\n나만의 시간!',
  SNACK: '달콤한 한입,\n기분 좋은 하루!',
  TOY: '오래오래\n함께할 친구!',
  OUTING: '좋은 사람들과\n더 특별한 시간!',
  PRIVILEGE: '오늘 하루는\n조금 더 특별하게!',
  ETC: '무엇이든\n설레는 선물!',
};

export function categoryTagline(category: StoreProductCategory): string {
  return CATEGORY_TAGLINES[category];
}
