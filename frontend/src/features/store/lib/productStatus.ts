import type { PurchaseHistoryItem } from '@/features/store/model/types';
import { todayDateKst } from '@/shared/lib/dateRange';

/** 카드 표시용 상품 상태 판단. quantity<=0 이면 품절, price===null 이면 AI 가격 산정 중. */
export function isSoldOut(quantity: number): boolean {
  return quantity <= 0;
}

export function isPriceReady(price: number | null): boolean {
  return price != null;
}

/** 오늘 하루의 구매 내역 조회 범위. 서버의 하루 1회 판정(purchase_date)과 같은 KST 기준이다. */
export function todayPurchaseRange(now?: number): { startDate: string; endDate: string } {
  const today = todayDateKst(now);
  return { startDate: today, endDate: today };
}

/**
 * 이 상품을 오늘 이미 샀는지 (SHOP-06 "한 상품당 하루 1회").
 *
 * <p>서버는 제한을 강제하지만(DAILY_LIMIT_EXCEEDED) 상품 응답에 "오늘 샀다" 는 정보가 없다.
 * 그래서 아이가 같은 상품을 또 누르면 에러를 받고 나서야 알았다 (QA). 오늘 범위의 구매
 * 내역으로 화면이 직접 판정해 버튼을 미리 막는다 — 서버가 최종 판단자인 것은 그대로다.
 *
 * <p>내역을 아직 못 받았으면(undefined) 산 적 없다고 본다. 살 수 있는 버튼을 잠깐 보여
 * 주는 쪽이, 살 수 있는데 못 산다고 막아 두는 쪽보다 낫다.
 */
export function purchasedToday(
  purchases: PurchaseHistoryItem[] | undefined,
  productId: number | undefined,
): boolean {
  if (purchases == null || productId == null) {
    return false;
  }
  return purchases.some((purchase) => purchase.storeProductId === productId);
}
