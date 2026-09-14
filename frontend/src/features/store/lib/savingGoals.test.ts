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
  it('excludes products already affordable or without a price yet', () => {
    const products = [
      product({ storeProductId: 1, price: 5000 }), // 이미 살 수 있음
      product({ storeProductId: 2, price: null }), // 가격 산정 중
      product({ storeProductId: 3, price: 15000 }),
    ];

    const goals = nearestSavingGoals(products, 8300);

    expect(goals).toHaveLength(1);
    expect(goals[0].product.storeProductId).toBe(3);
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
