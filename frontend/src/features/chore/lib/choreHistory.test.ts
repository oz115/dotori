import type { ChoreCompletionHistoryItem } from '@/features/chore/model/types';

import { matchCompletionPhotos, summarizeWeek, weekStart } from './choreHistory';

const done = (processedAt: string, rewardPoints = 100): ChoreCompletionHistoryItem => ({
  choreId: 1,
  choreTitle: '설거지 하기',
  childName: '민준',
  rewardPoints,
  processedAt,
});

describe('weekStart', () => {
  it('주 중간이면 그 주 월요일 0시다', () => {
    // 2026-09-16 은 수요일
    const start = weekStart(new Date(2026, 8, 16, 15, 30));

    expect(start.getDay()).toBe(1);
    expect(start.getDate()).toBe(14);
    expect(start.getHours()).toBe(0);
  });

  it('월요일이면 그날 0시다', () => {
    expect(weekStart(new Date(2026, 8, 14, 9, 0)).getDate()).toBe(14);
  });

  it('일요일은 지난 월요일이 시작이다', () => {
    // getDay() 를 그대로 쓰면 일요일에 이번 주가 통째로 비어 버린다.
    expect(weekStart(new Date(2026, 8, 20, 9, 0)).getDate()).toBe(14);
  });
});

describe('summarizeWeek', () => {
  const now = new Date(2026, 8, 16, 12, 0); // 수요일

  it('이번 주 것만 세고 더한다', () => {
    const summary = summarizeWeek(
      [
        done(new Date(2026, 8, 15, 18, 0).toISOString(), 100),
        done(new Date(2026, 8, 16, 9, 0).toISOString(), 50),
        done(new Date(2026, 8, 13, 9, 0).toISOString(), 500), // 지난 주
      ],
      now,
    );

    expect(summary).toEqual({ count: 2, total: 150 });
  });

  it('비어 있으면 0이다', () => {
    expect(summarizeWeek([], now)).toEqual({ count: 0, total: 0 });
  });
});

describe('matchCompletionPhotos', () => {
  function request(overrides: {
    choreCompletionRequestId: number;
    choreId: number;
    requestedAt: string;
    childId?: number;
    description?: string | null;
  }) {
    return {
      photoUrl: `https://s3/photo-${overrides.choreCompletionRequestId}.jpg?sig=abc`,
      description: null,
      ...overrides,
    };
  }

  it('지급 시각보다 앞선 요청 중 가장 최근 것을 붙인다', () => {
    const rows = matchCompletionPhotos(
      [{ choreId: 1, processedAt: '2026-09-20T10:00:00+09:00' }],
      [
        request({
          choreCompletionRequestId: 10,
          choreId: 1,
          requestedAt: '2026-09-18T09:00:00+09:00',
        }),
        request({
          choreCompletionRequestId: 11,
          choreId: 1,
          requestedAt: '2026-09-20T09:00:00+09:00',
        }),
      ],
    );

    expect(rows[0].photo?.requestId).toBe(11);
  });

  it('반복 미션은 회차마다 다른 사진이 붙는다', () => {
    // 쓴 요청을 빼지 않으면 가장 최근 사진이 모든 회차에 붙는다.
    const rows = matchCompletionPhotos(
      [
        { choreId: 1, processedAt: '2026-09-20T10:00:00+09:00' },
        { choreId: 1, processedAt: '2026-09-13T10:00:00+09:00' },
      ],
      [
        request({
          choreCompletionRequestId: 20,
          choreId: 1,
          requestedAt: '2026-09-13T09:00:00+09:00',
        }),
        request({
          choreCompletionRequestId: 21,
          choreId: 1,
          requestedAt: '2026-09-20T09:00:00+09:00',
        }),
      ],
    );

    expect(rows.map((row) => row.photo?.requestId)).toEqual([21, 20]);
  });

  it('아이가 섞여 있어도 남의 요청이 붙지 않는다', () => {
    const rows = matchCompletionPhotos(
      [
        { choreId: 1, childId: 7, processedAt: '2026-09-20T10:00:00+09:00' },
        { choreId: 1, childId: 8, processedAt: '2026-09-20T11:00:00+09:00' },
      ],
      [
        request({
          choreCompletionRequestId: 30,
          choreId: 1,
          childId: 7,
          requestedAt: '2026-09-20T09:00:00+09:00',
        }),
        request({
          choreCompletionRequestId: 31,
          choreId: 1,
          childId: 8,
          requestedAt: '2026-09-20T09:30:00+09:00',
        }),
      ],
    );

    expect(rows.map((row) => row.photo?.requestId)).toEqual([30, 31]);
  });

  it('짝이 없으면 null 이다 (지급보다 나중에 들어온 요청은 쓰지 않는다)', () => {
    const rows = matchCompletionPhotos(
      [{ choreId: 1, processedAt: '2026-09-20T10:00:00+09:00' }],
      [
        request({
          choreCompletionRequestId: 40,
          choreId: 2,
          requestedAt: '2026-09-20T09:00:00+09:00',
        }),
        request({
          choreCompletionRequestId: 41,
          choreId: 1,
          requestedAt: '2026-09-21T09:00:00+09:00',
        }),
      ],
    );

    expect(rows[0].photo).toBeNull();
  });

  it('원래 순서와 다른 필드는 그대로 둔다', () => {
    const rows = matchCompletionPhotos(
      [
        { choreId: 1, processedAt: '2026-09-13T10:00:00+09:00', choreTitle: '설거지' },
        { choreId: 2, processedAt: '2026-09-20T10:00:00+09:00', choreTitle: '빨래' },
      ],
      [],
    );

    expect(rows.map((row) => row.choreTitle)).toEqual(['설거지', '빨래']);
  });
});
