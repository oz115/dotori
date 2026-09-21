import {
  daysUntilMaturity,
  ongoingSavingsAccounts,
  previewSavingsAccounts,
  savingsMaturityMarker,
  summarizeSavingsAssets,
} from './summary';
import type { SavingsAccountPreviewItem } from './summary';
import type {
  SavingsProductType,
  SavingsSubscription,
  SavingsSubscriptionStatus,
} from '@/features/savings/model/types';

describe('daysUntilMaturity', () => {
  const now = new Date(2026, 8, 17, 15, 0);

  it('남은 날을 날짜 경계로 센다', () => {
    expect(daysUntilMaturity(new Date(2026, 8, 24, 1, 0).toISOString(), now)).toBe(7);
  });

  it('오늘이 만기면 0이다', () => {
    expect(daysUntilMaturity(new Date(2026, 8, 17, 23, 0).toISOString(), now)).toBe(0);
  });

  // 만기가 지난 건은 수령 대기 건수가 따로 알려 주므로 "D-음수"를 보여 줄 이유가 없다.
  it('이미 지난 만기는 null 이다', () => {
    expect(daysUntilMaturity(new Date(2026, 8, 16, 9, 0).toISOString(), now)).toBeNull();
  });

  it('만기가 없는 상품(보통예금)은 null 이다', () => {
    expect(daysUntilMaturity(null, now)).toBeNull();
  });
});

function sub(
  savingsSubscriptionId: number,
  productName: string,
  productType: SavingsProductType,
  status: SavingsSubscriptionStatus,
): SavingsSubscription {
  return {
    savingsSubscriptionId,
    savingsProductId: 1,
    productName,
    productType,
    contributionStyle: 'LUMP_SUM',
    installmentCount: null,
    principal: 10000,
    currentAmount: 10000,
    accruedInterest: 0,
    totalAmount: 10000,
    contributionCount: 0,
    status,
    startedAt: '2026-09-01T00:00:00+09:00',
    maturityAt: null,
    nextSettlementAt: null,
    receivedAt: null,
  };
}

describe('ongoingSavingsAccounts', () => {
  // 가입 목록 API 는 끝난 건까지 전부 내려준다. 그 건들은 이미 지갑으로 빠져나가
  // 아이의 예적금이 아니므로 카드가 세면 "3계좌"인데 금액은 2계좌 몫만 나온다.
  it('진행 중과 수령 대기만 남긴다', () => {
    const all = [
      sub(1, '보통예금', 'ORDINARY', 'ACTIVE'),
      sub(2, '중기적금', 'MID_TERM_INSTALLMENT', 'MATURED'),
      sub(3, '단리예금', 'SIMPLE_INTEREST', 'COMPLETED'),
      sub(4, '장기적금', 'LONG_TERM_INSTALLMENT', 'CANCELLED'),
    ];

    expect(ongoingSavingsAccounts(all).map((a) => a.savingsSubscriptionId)).toEqual([1, 2]);
  });

  it('가입이 없으면 빈 목록이다', () => {
    expect(ongoingSavingsAccounts([])).toEqual([]);
  });
});

describe('previewSavingsAccounts', () => {
  const accounts = [
    sub(1, '보통예금', 'ORDINARY', 'ACTIVE'),
    sub(2, '중기적금', 'MID_TERM_INSTALLMENT', 'ACTIVE'),
    sub(3, '장기적금', 'LONG_TERM_INSTALLMENT', 'ACTIVE'),
  ];

  it('기본 3개까지는 전부 보여주고 나머지는 없다', () => {
    const preview = previewSavingsAccounts(accounts);

    expect(preview.items.map((i) => i.productName)).toEqual(['보통예금', '중기적금', '장기적금']);
    expect(preview.restCount).toBe(0);
  });

  // 만기 표시를 정하려면 상태와 만기 시각이 같이 와야 한다.
  it('만기 표시에 필요한 상태와 만기 시각을 함께 넘긴다', () => {
    expect(previewSavingsAccounts(accounts).items[0]).toEqual({
      savingsSubscriptionId: 1,
      productName: '보통예금',
      productType: 'ORDINARY',
      status: 'ACTIVE',
      maturityAt: null,
    });
  });

  it('한도를 넘으면 앞쪽만 두고 나머지를 센다', () => {
    const many = [
      ...accounts,
      sub(4, '소액적금', 'SMALL_INSTALLMENT', 'ACTIVE'),
      sub(5, '복리예금', 'COMPOUND_INTEREST', 'ACTIVE'),
    ];

    const preview = previewSavingsAccounts(many);
    expect(preview.items.map((i) => i.productName)).toEqual(['보통예금', '중기적금', '장기적금']);
    expect(preview.restCount).toBe(2);
  });

  // 아이콘은 productType 으로 고른다. 이름만 넘기면 화면이 유형을 다시 찾아야 한다.
  it('아이콘을 고를 상품 유형을 함께 넘긴다', () => {
    expect(previewSavingsAccounts(accounts).items[0].productType).toBe('ORDINARY');
  });

  it('계좌가 없으면 빈 미리보기다', () => {
    expect(previewSavingsAccounts([])).toEqual({ items: [], restCount: 0 });
  });
});

describe('savingsMaturityMarker', () => {
  const now = new Date(2026, 8, 17, 15, 0);

  function account(
    status: SavingsSubscriptionStatus,
    maturityAt: string | null,
  ): SavingsAccountPreviewItem {
    return {
      savingsSubscriptionId: 1,
      productName: '장기적금',
      productType: 'LONG_TERM_INSTALLMENT',
      status,
      maturityAt,
    };
  }

  it('만기가 남았으면 남은 날을 센다', () => {
    const maturityAt = new Date(2026, 8, 29, 9, 0).toISOString();

    expect(savingsMaturityMarker(account('ACTIVE', maturityAt), now)).toEqual({
      kind: 'COUNTDOWN',
      days: 12,
    });
  });

  it('오늘이 만기면 D-0 이다', () => {
    const maturityAt = new Date(2026, 8, 17, 23, 0).toISOString();

    expect(savingsMaturityMarker(account('ACTIVE', maturityAt), now)).toEqual({
      kind: 'COUNTDOWN',
      days: 0,
    });
  });

  it('수령 대기 건은 남은 날 대신 만기로 표시한다', () => {
    const maturityAt = new Date(2026, 8, 10, 9, 0).toISOString();

    expect(savingsMaturityMarker(account('MATURED', maturityAt), now)).toEqual({ kind: 'MATURED' });
  });

  // 보통예금은 수시입출금이라 셀 날짜가 없다.
  it('만기가 없는 상품은 표시가 없다', () => {
    expect(savingsMaturityMarker(account('ACTIVE', null), now)).toBeNull();
  });

  // 정산 배치는 매일 00:20 에 돈다. 그 전까지는 만기가 지나도 상태가 ACTIVE 로 남는데,
  // 이때 "만기"로 적으면 부모는 수령을 기대하는데 아이는 아직 누를 수 없다.
  it('만기가 지났는데 아직 ACTIVE 면 표시를 비운다', () => {
    const maturityAt = new Date(2026, 8, 16, 9, 0).toISOString();

    expect(savingsMaturityMarker(account('ACTIVE', maturityAt), now)).toBeNull();
  });
});

// 서버는 OffsetDateTime 을 그대로 직렬화해 소수점 초가 6자리로 온다. Hermes 는 그 형태를
// Invalid Date 로 보기 때문에, 실기기에서만 D-day 가 사라지는 사고가 났다.
describe('savingsMaturityMarker — 서버 실제 응답 형태', () => {
  it('마이크로초가 붙은 만기 시각도 남은 날을 센다', () => {
    const account: SavingsAccountPreviewItem = {
      savingsSubscriptionId: 13,
      productName: '단리예금',
      productType: 'SIMPLE_INTEREST',
      status: 'ACTIVE',
      maturityAt: '2026-10-16T06:00:57.162979Z',
    };

    expect(savingsMaturityMarker(account, new Date(2026, 8, 18, 15, 30))).toEqual({
      kind: 'COUNTDOWN',
      days: 28,
    });
  });
});

describe('summarizeSavingsAssets', () => {
  it('진행 중과 수령 대기 건수를 합치고, 금액은 서버가 더한 원금 + 이자를 쓴다', () => {
    expect(summarizeSavingsAssets({ activeCount: 2, maturedCount: 1, totalAmount: 5300 })).toEqual({
      count: 3,
      totalAmount: 5300,
    });
  });

  it('가입한 건이 없으면 0건 0포인트다 — 가짜 값으로 채우지 않는다', () => {
    expect(summarizeSavingsAssets({ activeCount: 0, maturedCount: 0, totalAmount: 0 })).toEqual({
      count: 0,
      totalAmount: 0,
    });
  });

  it('수령 대기 건만 있어도 자산으로 센다', () => {
    expect(summarizeSavingsAssets({ activeCount: 0, maturedCount: 1, totalAmount: 1200 })).toEqual({
      count: 1,
      totalAmount: 1200,
    });
  });
});
