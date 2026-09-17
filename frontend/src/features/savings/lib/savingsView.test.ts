import {
  buildSavingsCards,
  daysUntil,
  estimateReturn,
  savingsGroupOf,
  toSavingsTransactionItem,
} from './savingsView';
import type {
  SavingsProductResponse,
  SavingsSubscription,
  SavingsTransactionResponse,
} from '@/features/savings/model/types';

const NOW = new Date('2026-09-17T12:00:00+09:00');

function product(overrides: Partial<SavingsProductResponse>): SavingsProductResponse {
  return {
    savingsProductId: 1,
    name: '단리예금',
    productType: 'SIMPLE_INTEREST',
    description: null,
    interestRate: 5,
    rewardRatio: 0.4,
    contributionStyle: 'LUMP_SUM',
    payoutTiming: 'MATURITY_LUMP_SUM',
    termWeeks: 4,
    installmentCount: null,
    educationGoal: null,
    preSignupMessage: null,
    maxAmount: 5000,
    ...overrides,
  };
}

function subscription(overrides: Partial<SavingsSubscription>): SavingsSubscription {
  return {
    savingsSubscriptionId: 10,
    savingsProductId: 1,
    productName: '단리예금',
    productType: 'SIMPLE_INTEREST',
    contributionStyle: 'LUMP_SUM',
    installmentCount: null,
    principal: 3000,
    currentAmount: 3000,
    accruedInterest: 150,
    totalAmount: 3150,
    contributionCount: 0,
    status: 'ACTIVE',
    startedAt: '2026-09-10T00:00:00+09:00',
    maturityAt: '2026-10-08T00:00:00+09:00',
    nextSettlementAt: null,
    receivedAt: null,
    ...overrides,
  };
}

describe('savingsGroupOf', () => {
  it('보통·단리·복리는 예금, 나머지는 적금이다', () => {
    expect(savingsGroupOf('ORDINARY')).toBe('DEPOSIT');
    expect(savingsGroupOf('COMPOUND_INTEREST')).toBe('DEPOSIT');
    expect(savingsGroupOf('SMALL_INSTALLMENT')).toBe('INSTALLMENT');
    expect(savingsGroupOf('GOAL_INSTALLMENT')).toBe('INSTALLMENT');
  });
});

describe('daysUntil', () => {
  it('만기까지 남은 날을 올림으로 세고, 지났으면 0이다', () => {
    expect(daysUntil('2026-09-18T00:00:00+09:00', NOW)).toBe(1);
    expect(daysUntil('2026-09-10T00:00:00+09:00', NOW)).toBe(0);
  });
});

describe('buildSavingsCards', () => {
  it('미가입 상품은 스펙(이자율·기간·최대 금액)을 싣고 진행바가 없다', () => {
    const [card] = buildSavingsCards([product({})], [], NOW);

    expect(card.subscribed).toBe(false);
    expect(card.progress).toBeUndefined();
    expect(card.lines).toEqual([
      { label: '이자율', value: '5%' },
      { label: '기간', value: '4주' },
      { label: '최대', value: '5,000P' },
    ]);
  });

  it('거치식 가입은 예치 금액·쌓인 이자·만기까지와 기간 진행률을 싣는다', () => {
    const [card] = buildSavingsCards([product({})], [subscription({})], NOW);

    expect(card.subscribed).toBe(true);
    expect(card.lines).toEqual([
      { label: '예치 금액', value: '3,000P' },
      { label: '쌓인 이자', value: '150P' },
      { label: '만기까지', value: '21일' },
    ]);
    expect(card.progress).toBeCloseTo(0.268, 2); // 28일 중 7.5일 경과
  });

  it('정액 적립식은 납입 회차 기준으로 진행률을 계산한다', () => {
    const [card] = buildSavingsCards(
      [product({ productType: 'SMALL_INSTALLMENT', contributionStyle: 'FIXED_INSTALLMENT' })],
      [
        subscription({
          productType: 'SMALL_INSTALLMENT',
          contributionStyle: 'FIXED_INSTALLMENT',
          installmentCount: 4,
          contributionCount: 1,
          principal: 500,
          currentAmount: 1000,
        }),
      ],
      NOW,
    );

    expect(card.lines[0]).toEqual({ label: '매주', value: '500P' });
    expect(card.lines[2]).toEqual({ label: '납입', value: '1회 / 4회' });
    expect(card.progress).toBe(0.25);
  });

  it('목표적금은 모은 금액 기준 진행률이고, 만기 도달은 matured 로 표시된다', () => {
    const [card] = buildSavingsCards(
      [product({ productType: 'GOAL_INSTALLMENT', contributionStyle: 'FLEXIBLE_GOAL' })],
      [
        subscription({
          productType: 'GOAL_INSTALLMENT',
          contributionStyle: 'FLEXIBLE_GOAL',
          principal: 2000,
          currentAmount: 1800,
          status: 'MATURED',
        }),
      ],
      NOW,
    );

    expect(card.matured).toBe(true);
    expect(card.progress).toBe(0.9);
    expect(card.lines[2]).toEqual({ label: '만기', value: '달성!' });
  });

  it('해지·수령 완료된 가입은 보유로 치지 않는다', () => {
    const [card] = buildSavingsCards([product({})], [subscription({ status: 'COMPLETED' })], NOW);

    expect(card.subscribed).toBe(false);
  });
});

describe('estimateReturn — 서버 정산과 같은 식이어야 화면 약속이 지켜진다', () => {
  it('단리예금은 매주 (원금 × 기간 이자율 ÷ 기간)을 기간만큼 쌓는다', () => {
    const result = estimateReturn(product({ interestRate: 15, termWeeks: 4 }), 4000);

    // round(4000 × 15% ÷ 4) = 150 씩 4주
    expect(result).toEqual({ principal: 4000, interest: 600, total: 4600, weekly: false });
  });

  it('복리예금은 쌓인 이자에도 매주 이자가 붙는다', () => {
    const result = estimateReturn(
      product({ productType: 'COMPOUND_INTEREST', interestRate: 10, termWeeks: 4 }),
      1000,
    );

    // 100 → 110 → 121 → 133 (매주 반올림)
    expect(result.interest).toBe(464);
    expect(result.total).toBe(1464);
  });

  it('정액 적립식은 납입으로 늘어난 잔액 기준으로 이자를 쌓는다', () => {
    const result = estimateReturn(
      product({
        productType: 'SMALL_INSTALLMENT',
        contributionStyle: 'FIXED_INSTALLMENT',
        interestRate: 10,
        termWeeks: 2,
        installmentCount: 2,
      }),
      500,
    );

    // 1주차 round(500×10%÷2)=25, 2주차 round(1000×5%)=50
    expect(result).toEqual({ principal: 1000, interest: 75, total: 1075, weekly: false });
  });

  it('목표적금은 목표 금액 기준 이자를 한 번에 받는다', () => {
    const result = estimateReturn(
      product({
        productType: 'GOAL_INSTALLMENT',
        contributionStyle: 'FLEXIBLE_GOAL',
        interestRate: 15,
      }),
      2000,
    );

    expect(result).toEqual({ principal: 2000, interest: 300, total: 2300, weekly: false });
  });

  it('보통예금은 만기 대신 매주 받는 이자 한 주치를 준다', () => {
    const result = estimateReturn(
      product({
        productType: 'ORDINARY',
        contributionStyle: 'FREE',
        interestRate: 1,
        termWeeks: null,
      }),
      500,
    );

    expect(result.weekly).toBe(true);
    expect(result.interest).toBe(5);
  });
});

describe('toSavingsTransactionItem', () => {
  function transaction(overrides: Partial<SavingsTransactionResponse>): SavingsTransactionResponse {
    return {
      savingsTransactionId: 1,
      savingsSubscriptionId: 10,
      savingsProductId: 1,
      productName: '단리예금',
      productType: 'SIMPLE_INTEREST',
      transactionType: 'INITIAL_DEPOSIT',
      amount: 3000,
      createdAt: '2026-09-17T09:00:00+09:00',
      ...overrides,
    };
  }

  it('납입은 지갑에서 나간 돈(-)으로, 수령은 들어온 돈(+)으로 표시한다', () => {
    expect(toSavingsTransactionItem(transaction({}))).toMatchObject({
      badge: '넣었어요',
      sign: '-',
      date: '2026. 09. 17',
    });
    expect(
      toSavingsTransactionItem(transaction({ transactionType: 'MATURITY_PAYOUT' })),
    ).toMatchObject({ badge: '만기 수령', sign: '+' });
    expect(toSavingsTransactionItem(transaction({ transactionType: 'INTEREST' }))).toMatchObject({
      badge: '이자 받았어요',
      sign: '+',
    });
  });
});
