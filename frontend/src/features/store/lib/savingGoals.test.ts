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

const noSeen: ReadonlySet<number> = new Set();

describe('nearestSavingGoals', () => {
  it('처음부터 살 수 있던 상품과 가격 산정 중인 상품은 띄우지 않는다', () => {
    const products = [
      product({ storeProductId: 1, price: 5000 }), // 목표였던 적이 없다
      product({ storeProductId: 2, price: null }), // 가격 산정 중
      product({ storeProductId: 3, price: 15000 }),
    ];

    const goals = nearestSavingGoals(products, 8300, noSeen);

    expect(goals).toHaveLength(1);
    expect(goals[0].product.storeProductId).toBe(3);
  });

  it('한 번 목표로 떠 있던 상품은 살 수 있게 돼도 달성 상태로 맨 앞에 남는다', () => {
    const products = [
      product({ storeProductId: 1, price: 5000 }), // 예전에 목표였다가 잔액이 넘었다
      product({ storeProductId: 3, price: 15000 }),
    ];

    const goals = nearestSavingGoals(products, 8300, new Set([1]));

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

    const goals = nearestSavingGoals(products, 8300, noSeen);

    expect(goals.map((g) => g.product.storeProductId)).toEqual([2, 1]);
    expect(goals[0].remaining).toBe(6700);
    expect(goals[0].progress).toBeCloseTo(8300 / 15000);
  });

  it('caps progress at 1 and respects the limit', () => {
    const products = Array.from({ length: 3 }, (_, i) =>
      product({ storeProductId: i + 1, price: 9000 + i * 100 }),
    );

    const goals = nearestSavingGoals(products, 8300, noSeen, 2);

    expect(goals).toHaveLength(2);
    expect(goals.every((g) => g.progress <= 1)).toBe(true);
  });
});
