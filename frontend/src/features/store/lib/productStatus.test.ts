import type { PurchaseHistoryItem } from '@/features/store/model/types';

import { isPriceReady, isSoldOut, purchasedToday, todayPurchaseRange } from './productStatus';

function purchase(storeProductId: number): PurchaseHistoryItem {
  return {
    purchaseId: 1,
    storeProductId,
    productName: '포켓몬 카드',
    category: 'TOY',
    quantity: 1,
    totalPrice: 300,
    purchasedAt: '2026-09-22T10:00:00+09:00',
  };
}

describe('isSoldOut', () => {
  it('재고가 0이면 품절이다', () => {
    expect(isSoldOut(0)).toBe(true);
  });

  it('재고가 1 이상이면 품절이 아니다', () => {
    expect(isSoldOut(1)).toBe(false);
  });
});

describe('isPriceReady', () => {
  it('가격이 null이면 산정 전이다', () => {
    expect(isPriceReady(null)).toBe(false);
  });

  it('가격이 있으면 산정 완료다', () => {
    expect(isPriceReady(100)).toBe(true);
  });
});

describe('todayPurchaseRange', () => {
  it('시작·끝이 같은 하루 범위이고 KST 기준이다', () => {
    // 기기 시간대가 KST 가 아니어도 서버가 보는 "오늘"과 같아야 한다 — UTC 18시는 KST 다음 날.
    expect(todayPurchaseRange(Date.parse('2026-09-21T18:00:00Z'))).toEqual({
      startDate: '2026-09-22',
      endDate: '2026-09-22',
    });
  });
});

describe('purchasedToday', () => {
  it('오늘 내역에 그 상품이 있으면 이미 산 것이다', () => {
    expect(purchasedToday([purchase(5)], 5)).toBe(true);
  });

  it('다른 상품만 샀으면 이 상품은 살 수 있다', () => {
    // 제한은 상품 단위다 (한 상품당 하루 1회) — 상점 전체가 막히면 안 된다.
    expect(purchasedToday([purchase(7)], 5)).toBe(false);
  });

  it('내역을 아직 못 받았으면 살 수 있다고 본다', () => {
    // 살 수 있는데 막아 두는 쪽이 더 나쁘다. 서버가 최종 판단자다.
    expect(purchasedToday(undefined, 5)).toBe(false);
  });

  it('상품 id 를 모르면 살 수 있다고 본다', () => {
    expect(purchasedToday([purchase(5)], undefined)).toBe(false);
  });
});
