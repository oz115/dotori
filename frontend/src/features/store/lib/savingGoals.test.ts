import { nearestSavingGoals } from './savingGoals';
import type { StoreProductSummary } from '@/features/store/model/types';

function product(overrides: Partial<StoreProductSummary>): StoreProductSummary {
  return {
    storeProductId: 1,
    name: '놀이공원 가기',
    description: null,
    category: 'OUTING',
    price: 15000,
    quantity: 1,
    createdAt: '2026-09-01T00:00:00+09:00',
    ...overrides,
  };
}

describe('nearestSavingGoals', () => {
  it('살 수 있게 된 상품은 빼지 않고 달성 상태로 맨 앞에 둔다 — 가격 산정 중만 뺀다', () => {
    const products = [
      product({ storeProductId: 1, price: 5000 }), // 이미 살 수 있음 — 사라지면 달성한 걸 모른다
      product({ storeProductId: 2, price: null }), // 가격 산정 중
      product({ storeProductId: 3, price: 15000 }),
    ];

    const goals = nearestSavingGoals(products, 8300);

    expect(goals.map((g) => g.product.storeProductId)).toEqual([1, 3]);
    expect(goals[0].achievable).toBe(true);
    expect(goals[0].remaining).toBe(0);
    expect(goals[0].progress).toBe(1);
    expect(goals[1].achievable).toBe(false);
  });

  it('sorts by remaining amount ascending and computes progress', () => {
    const products = [
      product({ storeProductId: 1, price: 20000 }),
      product({ storeProductId: 2, price: 15000 }),
    ];

    const goals = nearestSavingGoals(products, 8300);

    expect(goals.map((g) => g.product.storeProductId)).toEqual([2, 1]);
    expect(goals[0].remaining).toBe(6700);
    expect(goals[0].progress).toBeCloseTo(8300 / 15000);
  });

  it('caps progress at 1 and respects the limit', () => {
    const products = Array.from({ length: 3 }, (_, i) =>
      product({ storeProductId: i + 1, price: 9000 + i * 100 }),
    );

    const goals = nearestSavingGoals(products, 8300, 2);

    expect(goals).toHaveLength(2);
    expect(goals.every((g) => g.progress <= 1)).toBe(true);
  });
});
