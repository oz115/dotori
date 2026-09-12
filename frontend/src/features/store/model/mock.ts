import type { StoreProductSummary } from '@/features/store/model/types';

/**
 * 화면 확인용 예시 상품.
 *
 * 부모가 상점에 물건을 올리기 전에는 "조금만 더 모으면!" 이 통째로 사라져서
 * 배치를 볼 수 없어, 그럴 때만 자리에 채워 넣는다. 실제 상품이 있으면 쓰지 않는다.
 */
const CREATED_AT = '2026-09-01T09:00:00+09:00';

export const mockStoreProducts: StoreProductSummary[] = [
  {
    storeProductId: -1,
    name: '놀이공원 가기',
    category: 'OUTING',
    price: 15_000,
    quantity: 1,
    createdAt: CREATED_AT,
  },
  {
    storeProductId: -2,
    name: '무선 이어폰',
    category: 'TOY',
    price: 20_000,
    quantity: 1,
    createdAt: CREATED_AT,
  },
  {
    storeProductId: -3,
    name: '치킨 먹기',
    category: 'SNACK',
    price: 3_000,
    quantity: 1,
    createdAt: CREATED_AT,
  },
];
