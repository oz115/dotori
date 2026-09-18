import type { CashoutRequest } from '@/features/wallet/model/types';

import {
  cashoutResultNote,
  cashoutStatusLabel,
  cashoutStatusTone,
  isPendingCashout,
  splitCashoutRequests,
} from './cashoutStatus';

function request(over: Partial<CashoutRequest>): CashoutRequest {
  return {
    cashoutRequestId: 1,
    childId: 1,
    parentId: 2,
    pointAmount: 1_000,
    status: 'PENDING',
    requestedAt: '2026-09-10T10:00:00+09:00',
    processedAt: null,
    ...over,
  };
}

describe('cashoutStatusLabel', () => {
  it('tells apart who ended the request', () => {
    expect(cashoutStatusLabel('CANCELLED')).toBe('아이가 취소');
    expect(cashoutStatusLabel('DECLINED')).toBe('거절');
  });
});

describe('cashoutStatusTone', () => {
  it('아직 아무 일도 안 일어난 대기 중은 회색으로 물러난다', () => {
    expect(cashoutStatusTone('PENDING')).toBe('neutral');
  });

  it('부모가 끝낸 거절은 눈에 걸리게 색을 준다', () => {
    expect(cashoutStatusTone('DECLINED')).toBe('warning');
  });
});

describe('cashoutResultNote', () => {
  it('아직 안 끝난 요청에는 결과가 없다', () => {
    expect(cashoutResultNote('PENDING')).toBeNull();
    expect(cashoutResultNote('PROGRESS')).toBeNull();
  });

  it('도토리가 돌아갔는지로 결과를 말해 준다', () => {
    expect(cashoutResultNote('SUCCEED')).toContain('송금이 끝났어요');
    expect(cashoutResultNote('FAILED')).toContain('돌려줬어요');
    expect(cashoutResultNote('DECLINED')).toContain('돌려줬어요');
    expect(cashoutResultNote('CANCELLED')).toContain('돌려줬어요');
  });
});

describe('isPendingCashout', () => {
  it('counts only PENDING as something to handle', () => {
    expect(isPendingCashout('PENDING')).toBe(true);
    expect(isPendingCashout('PROGRESS')).toBe(false);
    expect(isPendingCashout('SUCCEED')).toBe(false);
  });
});

describe('splitCashoutRequests', () => {
  const older = request({ cashoutRequestId: 1, requestedAt: '2026-09-10T10:00:00+09:00' });
  const newer = request({ cashoutRequestId: 2, requestedAt: '2026-09-12T10:00:00+09:00' });
  const done = request({
    cashoutRequestId: 3,
    status: 'SUCCEED',
    requestedAt: '2026-09-09T10:00:00+09:00',
    processedAt: '2026-09-09T11:00:00+09:00',
  });
  const declined = request({
    cashoutRequestId: 4,
    status: 'DECLINED',
    requestedAt: '2026-09-11T10:00:00+09:00',
    processedAt: '2026-09-11T12:00:00+09:00',
  });

  it('puts the longest waiting request first', () => {
    const { pending } = splitCashoutRequests([newer, older]);
    expect(pending.map((item) => item.cashoutRequestId)).toEqual([1, 2]);
  });

  it('puts the most recently handled request first in history', () => {
    const { history } = splitCashoutRequests([done, declined]);
    expect(history.map((item) => item.cashoutRequestId)).toEqual([4, 3]);
  });

  it('keeps handled requests out of the pending list', () => {
    const { pending, history } = splitCashoutRequests([older, done, declined]);
    expect(pending).toHaveLength(1);
    expect(history).toHaveLength(2);
  });
});
