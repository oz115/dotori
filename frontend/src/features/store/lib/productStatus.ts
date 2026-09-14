/** 카드 표시용 상품 상태 판단. quantity<=0 이면 품절, price===null 이면 AI 가격 산정 중. */
export function isSoldOut(quantity: number): boolean {
  return quantity <= 0;
}

export function isPriceReady(price: number | null): boolean {
  return price != null;
}
