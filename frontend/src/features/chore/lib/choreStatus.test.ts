import type {
  ChoreCompletionHistoryItem,
  ChoreCompletionSummary,
  ChoreSummary,
} from '@/features/chore/model/types';

import { earliestStartDate, periodStartedAt, withStatus } from './choreStatus';

const chore = (choreId: number, startDate: string): ChoreSummary => ({
  choreId,
  childId: 1,
  title: `할 일 ${choreId}`,
  difficulty: 'NORMAL',
  rewardPoints: 500,
  repeatType: 'DAY',
  startDate,
  endDate: startDate,
  createdAt: `${startDate}T09:00:00+09:00`,
});

const pending = (choreId: number): ChoreCompletionSummary => ({
  choreCompletionRequestId: choreId * 10,
  choreId,
  choreTitle: `할 일 ${choreId}`,
  childName: '민준',
  photoUrl: '',
  description: null,
  status: 'PENDING',
  requestedAt: '2026-09-15T18:00:00+09:00',
});

const completion = (choreId: number, processedAt: string): ChoreCompletionHistoryItem => ({
  choreId,
  choreTitle: `할 일 ${choreId}`,
  childName: '민준',
  rewardPoints: 500,
  processedAt,
});

describe('periodStartedAt', () => {
  it('KST 자정 기준이다', () => {
    expect(periodStartedAt('2026-09-15')).toBe(Date.parse('2026-09-15T00:00:00+09:00'));
  });

  it('UTC 자정으로 계산하지 않는다 (아홉 시간 차이)', () => {
    expect(periodStartedAt('2026-09-15')).not.toBe(Date.parse('2026-09-15T00:00:00Z'));
  });
});

describe('earliestStartDate', () => {
  it('가장 이른 시작일을 고른다', () => {
    expect(earliestStartDate([chore(1, '2026-09-15'), chore(2, '2026-09-01')])).toBe('2026-09-01');
  });

  it('빈 목록이면 undefined 다', () => {
    expect(earliestStartDate([])).toBeUndefined();
  });
});

describe('withStatus', () => {
  it('아무것도 없으면 TODO 다', () => {
    expect(withStatus([chore(1, '2026-09-15')], [], [])[0].status).toBe('TODO');
  });

  it('대기 중인 요청이 있으면 PENDING 이다', () => {
    expect(withStatus([chore(1, '2026-09-15')], [pending(1)], [])[0].status).toBe('PENDING');
  });

  it('이번 주기에 지급됐으면 DONE 이고 PENDING 보다 우선한다', () => {
    const items = withStatus(
      [chore(1, '2026-09-15')],
      [pending(1)],
      [completion(1, '2026-09-15T18:30:00+09:00')],
    );

    expect(items[0].status).toBe('DONE');
  });

  it('며칠 전에 승인된 매주 반복도 DONE 으로 잡는다', () => {
    // 오늘 하루만 조회하던 시절에는 이 건이 빠져서 이미 받은 할 일이 TODO 로 보였다.
    const weekly = { ...chore(1, '2026-09-14'), repeatType: 'WEEK' as const };
    const items = withStatus([weekly], [], [completion(1, '2026-09-14T10:00:00+09:00')]);

    expect(items[0].status).toBe('DONE');
  });

  it('지난 주기의 지급은 세지 않는다', () => {
    // 크론이 기간을 밀면 startDate 가 앞으로 가므로, 그 이전 지급은 이번 주기가 아니다.
    const items = withStatus(
      [chore(1, '2026-09-15')],
      [],
      [completion(1, '2026-09-14T23:59:00+09:00')],
    );

    expect(items[0].status).toBe('TODO');
  });

  it('다른 할 일의 지급에 영향받지 않는다', () => {
    const items = withStatus(
      [chore(1, '2026-09-15'), chore(2, '2026-09-15')],
      [],
      [completion(2, '2026-09-15T12:00:00+09:00')],
    );

    expect(items.map((item) => item.status)).toEqual(['TODO', 'DONE']);
  });
});
