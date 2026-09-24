import type { StoreProductCategory } from '@/features/store/model/types';

/**
 * 카테고리 한글 표시명. 목록·상세의 배지는 영문 코드를 그대로 쓰지만(디자인 시안 기준),
 * 상품 등록 폼의 카테고리 선택지처럼 사용자가 직접 고르는 자리는 한글이 더 명확하다.
 */
const CATEGORY_LABELS: Record<StoreProductCategory, string> = {
  GAME: '게임',
  SNACK: '간식',
  TOY: '장난감',
  OUTING: '외출',
  PRIVILEGE: '특권',
  ETC: '기타',
};

export function categoryLabel(category: StoreProductCategory): string {
  return CATEGORY_LABELS[category];
}
