import { isSoldOut } from '@/features/store/lib/productStatus';
import type { StoreProductSummary } from '@/features/store/model/types';

export type SavingGoal = {
  product: StoreProductSummary & { price: number };
  /** 목표가까지 남은 포인트. 살 수 있으면 0 */
  remaining: number;
  /** 0~1. balance 가 price 를 넘으면 1로 고정 */
  progress: number;
  /** 잔액이 목표가에 닿아 지금 바로 살 수 있는 상품 */
  achievable: boolean;
};

/**
 * 자녀 홈 "조금만 더 모으면!" 카드용. 목표가에 가장 가까운(남은 포인트가 적은) 것부터
 * 정렬한다 — 잔액이 목표가에 닿은 상품은 남은 값이 0이라 자연히 맨 앞에 온다.
 *
 * 지금 잔액으로 못 사는(price > balance) 상품이 목표로 뜬다. 모으던 목표가 잔액이
 * 차는 순간 조용히 사라지면 아이가 달성한 걸 모르고 지나가므로, **한 번이라도 목표로
 * 떠 있던 상품(seenGoalIds)** 은 살 수 있게 된 뒤에도 "이제 살 수 있어요!"로 남긴다.
 * 처음부터 살 수 있던 상품은 목표였던 적이 없으니 띄우지 않고, 가격이 아직 산정 중
 * (price === null)이거나 품절(quantity <= 0)된 상품도 뺀다 — 품절은 살 수 없는데
 * "이제 살 수 있어요"로 남아 있으면 거짓말이 된다.
 */
export function nearestSavingGoals(
  products: StoreProductSummary[],
  balance: number,
  seenGoalIds: ReadonlySet<number>,
  limit = 5,
): SavingGoal[] {
  return products
    .filter((p): p is StoreProductSummary & { price: number } => p.price != null)
    .filter((p) => !isSoldOut(p.quantity))
    .filter((p) => p.price > balance || seenGoalIds.has(p.storeProductId))
    .map((product) => ({
      product,
      remaining: Math.max(0, product.price - balance),
      progress: Math.min(1, balance / product.price),
      achievable: product.price <= balance,
    }))
    .sort((a, b) => a.remaining - b.remaining)
    .slice(0, limit);
}
