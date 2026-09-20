import { toTransactionItems } from './transactions';
import type { TransactionResponse } from '@/features/investment/model/types';

function row(
  id: number,
  type: 'BUY' | 'SELL',
  price: number,
  overrides: Partial<TransactionResponse> = {},
): TransactionResponse {
  return {
    investmentTransactionId: id,
    productName: '고소한빵',
    transactionType: type,
    unitPrice: price,
    quantity: 1,
    totalAmount: price,
    createdAt: `2026-09-19T10:00:0${id}.123456+09:00`,
    ...overrides,
  };
}

it('구버전 서버에서 고소한빵을 같은 가격에 사고팔면 수익은 0이다', () => {
  const [sale] = toTransactionItems([row(2, 'SELL', 10000), row(1, 'BUY', 10000)]);
  expect(sale.realizedProfit).toBe(0);
  expect(sale.buyAmount).toBe(10000);
});

it('재매매하면 이전 매수 원가를 재사용하지 않는다', () => {
  const [sale] = toTransactionItems([
    row(4, 'SELL', 900),
    row(3, 'BUY', 1100),
    row(2, 'SELL', 1050),
    row(1, 'BUY', 1000),
  ]);
  expect(sale.realizedProfit).toBe(-200);
});

it('다른 회사의 매수로 원가를 채우지 않는다', () => {
  const [sale] = toTransactionItems([
    row(2, 'SELL', 1000),
    row(1, 'BUY', 1000, { productName: '하늘항공' }),
  ]);
  expect(sale.realizedProfit).toBeUndefined();
});

it('새 서버가 보낸 손익 0과 명시적 미확정 null을 유지한다', () => {
  expect(
    toTransactionItems([row(2, 'SELL', 1000, { realizedProfit: 0, buyPrice: 1000 })])[0]
      .realizedProfit,
  ).toBe(0);
  expect(
    toTransactionItems([row(2, 'SELL', 1000, { realizedProfit: null }), row(1, 'BUY', 1000)])[0]
      .realizedProfit,
  ).toBeNull();
});

it('부분 매도는 실제 매도 수량만큼 원가를 차감한다', () => {
  const sales = toTransactionItems([
    row(3, 'SELL', 120),
    row(2, 'SELL', 100),
    row(1, 'BUY', 100, { quantity: 2, totalAmount: 200 }),
  ]);
  expect(sales[0].realizedProfit).toBe(20);
  expect(sales[1].realizedProfit).toBe(0);
});
