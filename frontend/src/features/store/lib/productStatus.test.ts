import { isPriceReady, isSoldOut } from './productStatus';

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
