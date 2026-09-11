import type { StoreProductSummary } from '@/features/store/model/types';

export type SavingGoal = {
  product: StoreProductSummary & { price: number };
  /** 목표가까지 남은 포인트 */
  remaining: number;
  /** 0~1. balance 가 price 를 넘으면 1로 고정 */
  progress: number;
};

/**
 * 자녀 홈 "조금만 더 모으면!" 카드용. 아직 잔액으로 못 사는 상품 중,
 * 목표가에 가장 가까운(남은 포인트가 적은) 것부터 정렬한다.
 *
 * 가격이 아직 산정 중(price === null)이거나 이미 살 수 있는(price <= balance) 상품은 뺀다 —
 * 전자는 "얼마나 모아야 하는지"를 보여줄 수 없고, 후자는 "이미 살 수 있어요" 쪽 안내라
 * 이 카드의 몫이 아니다.
 */
export function nearestSavingGoals(
  products: StoreProductSummary[],
  balance: number,
  limit = 5,
): SavingGoal[] {
  return products
    .filter(
      (p): p is StoreProductSummary & { price: number } => p.price != null && p.price > balance,
    )
    .map((product) => ({
      product,
      remaining: product.price - balance,
      progress: Math.min(1, balance / product.price),
    }))
    .sort((a, b) => a.remaining - b.remaining)
    .slice(0, limit);
}
