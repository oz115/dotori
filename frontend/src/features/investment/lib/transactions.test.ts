import {
  hasTransactionsBefore,
  recentTransactions,
  summarizeRealizedProfit,
  toTransactionItems,
  transactionPeriod,
  transactionsInPeriod,
} from './transactions';
import type {
  InvestmentTransactionItem,
  TransactionResponse,
} from '@/features/investment/model/types';

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

describe('recentTransactions', () => {
  const item = (id: number, createdAt: string): InvestmentTransactionItem => ({
    id,
    productName: '고소한빵',
    image: 0,
    type: 'BUY',
    totalAmount: 1000,
    date: '',
    createdAt,
  });
  // 2026-09-21(월) 낮 12시 KST
  const now = new Date('2026-09-21T12:00:00+09:00');

  it('오늘을 포함한 최근 7일(9/15~9/21)의 거래만 남기고 순서는 그대로 둔다', () => {
    const items = [
      item(1, '2026-09-21T00:00:01+09:00'),
      item(2, '2026-09-15T00:00:00+09:00'),
      item(3, '2026-09-14T23:59:59+09:00'),
    ];
    expect(recentTransactions(items, 7, now).map((i) => i.id)).toEqual([1, 2]);
  });

  it('KST 자정 경계는 UTC 가 아니라 한국 날짜로 자른다', () => {
    // UTC 로는 9/14 15:30 이지만 KST 로는 9/15 00:30 이라 포함된다
    const items = [item(1, '2026-09-14T15:30:00.000Z')];
    expect(recentTransactions(items, 7, now)).toHaveLength(1);
  });

  it('시각을 읽을 수 없는 거래는 제외한다', () => {
    expect(recentTransactions([item(1, '')], 7, now)).toEqual([]);
  });
});

describe('summarizeRealizedProfit', () => {
  const sale = (id: number, realizedProfit: number | null | undefined) =>
    ({ id, type: 'SELL', realizedProfit }) as InvestmentTransactionItem;

  it('매수는 세지 않고 판매의 확정 손익만 합산한다', () => {
    const buy = { id: 9, type: 'BUY' } as InvestmentTransactionItem;
    expect(summarizeRealizedProfit([buy, sale(1, 300), sale(2, -100)])).toEqual({
      count: 2,
      total: 200,
      incomplete: false,
    });
  });

  it('원가를 모르는 판매가 하나라도 있으면 불완전으로 표시한다', () => {
    expect(summarizeRealizedProfit([sale(1, 300), sale(2, null)]).incomplete).toBe(true);
    expect(summarizeRealizedProfit([sale(1, undefined)]).incomplete).toBe(true);
  });

  it('손익 0은 원가를 아는 확정값이라 불완전이 아니다', () => {
    expect(summarizeRealizedProfit([sale(1, 0)])).toEqual({
      count: 1,
      total: 0,
      incomplete: false,
    });
  });

  it('판매가 없으면 건수 0으로 알려준다', () => {
    expect(summarizeRealizedProfit([])).toEqual({ count: 0, total: 0, incomplete: false });
  });
});

describe('transactionPeriod / transactionsInPeriod / hasTransactionsBefore', () => {
  const item = (id: number, createdAt: string): InvestmentTransactionItem => ({
    id,
    productName: '고소한빵',
    image: 0,
    type: 'BUY',
    totalAmount: 1000,
    date: '',
    createdAt,
  });
  // 2026-09-21(월) 낮 12시 KST
  const now = new Date('2026-09-21T12:00:00+09:00');

  it('offset 0 은 오늘을 포함한 최근 7일이고 날짜 범위를 라벨로 준다', () => {
    expect(transactionPeriod(7, 0, now).label).toBe('2026.09.15 ~ 2026.09.21');
  });

  it('offset 이 늘면 7일씩 앞으로 밀린다 (빈틈도 겹침도 없다)', () => {
    expect(transactionPeriod(7, 1, now).label).toBe('2026.09.08 ~ 2026.09.14');
    expect(transactionPeriod(7, 2, now).label).toBe('2026.09.01 ~ 2026.09.07');
    // 월이 바뀌어도 이어진다
    expect(transactionPeriod(7, 3, now).label).toBe('2026.08.25 ~ 2026.08.31');
  });

  it('기간 경계 거래는 정확히 한 기간에만 들어간다', () => {
    const items = [
      item(1, '2026-09-15T00:00:00+09:00'), // 최근 7일의 첫 순간
      item(2, '2026-09-14T23:59:59+09:00'), // 그 바로 앞 기간의 마지막 순간
    ];
    expect(transactionsInPeriod(items, transactionPeriod(7, 0, now)).map((i) => i.id)).toEqual([1]);
    expect(transactionsInPeriod(items, transactionPeriod(7, 1, now)).map((i) => i.id)).toEqual([2]);
  });

  it('이 기간보다 오래된 거래가 있을 때만 이전으로 갈 수 있다', () => {
    const items = [item(1, '2026-09-20T10:00:00+09:00'), item(2, '2026-09-10T10:00:00+09:00')];
    expect(hasTransactionsBefore(items, transactionPeriod(7, 0, now))).toBe(true);
    expect(hasTransactionsBefore(items, transactionPeriod(7, 1, now))).toBe(false);
  });

  it('시각을 읽을 수 없는 거래는 어느 기간에도 넣지 않고 이전 기간 판단에도 쓰지 않는다', () => {
    const items = [item(1, '')];
    expect(transactionsInPeriod(items, transactionPeriod(7, 0, now))).toEqual([]);
    expect(hasTransactionsBefore(items, transactionPeriod(7, 0, now))).toBe(false);
  });
});
