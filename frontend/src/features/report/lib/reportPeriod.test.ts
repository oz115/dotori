import {
  currentWeek,
  daysOfWeek,
  formatWeekLabel,
  isCurrentWeek,
  isFutureDate,
  shiftWeek,
  todayInKst,
  weekOf,
} from './reportPeriod';

/**
 * 주 경계는 시간대 때문에 하루씩 밀리기 쉬운 계산이다. 실제로 이 화면을 설계하면서
 * `+09:00` 로 만든 Date 에 `getUTCDay()` 를 부르는 실수를 한 번 했다 — 그래서 요일이
 * 하루 앞으로 밀려 나왔다. 여기가 그것을 막는 자리다.
 *
 * 기준일: 2026-09-18 은 금요일, 그 주는 9/14(월) ~ 9/20(일) 이다.
 */
describe('weekOf', () => {
  it('주 중간 날짜에서 그 주의 월~일을 찾는다', () => {
    expect(weekOf('2026-09-18')).toEqual({ startDate: '2026-09-14', endDate: '2026-09-20' });
  });

  it('월요일을 주면 그 주가 그대로 나온다 — 한 주 앞으로 밀리지 않는다', () => {
    expect(weekOf('2026-09-14')).toEqual({ startDate: '2026-09-14', endDate: '2026-09-20' });
  });

  // 일요일이 주의 끝이라는 것이 여기서 갈린다. 일요일 시작 주로 잡으면 다음 주가 나온다.
  it('일요일을 주면 그 주의 끝으로 본다', () => {
    expect(weekOf('2026-09-20')).toEqual({ startDate: '2026-09-14', endDate: '2026-09-20' });
  });

  it('월을 넘는 주도 이어서 계산한다', () => {
    // 2026-10-01 은 목요일 → 그 주는 9/28(월) ~ 10/4(일)
    expect(weekOf('2026-10-01')).toEqual({ startDate: '2026-09-28', endDate: '2026-10-04' });
  });

  it('연말을 넘는 주도 이어서 계산한다', () => {
    // 2027-01-01 은 금요일 → 그 주는 2026-12-28(월) ~ 2027-01-03(일)
    expect(weekOf('2027-01-01')).toEqual({ startDate: '2026-12-28', endDate: '2027-01-03' });
  });
});

describe('daysOfWeek', () => {
  it('월요일부터 일곱 날짜를 순서대로 준다', () => {
    expect(daysOfWeek({ startDate: '2026-09-14', endDate: '2026-09-20' })).toEqual([
      '2026-09-14',
      '2026-09-15',
      '2026-09-16',
      '2026-09-17',
      '2026-09-18',
      '2026-09-19',
      '2026-09-20',
    ]);
  });
});

describe('shiftWeek', () => {
  const week = { startDate: '2026-09-14', endDate: '2026-09-20' };

  it('지난 주로 이동한다', () => {
    expect(shiftWeek(week, -1)).toEqual({ startDate: '2026-09-07', endDate: '2026-09-13' });
  });

  it('다음 주로 이동한다', () => {
    expect(shiftWeek(week, 1)).toEqual({ startDate: '2026-09-21', endDate: '2026-09-27' });
  });

  it('여러 주를 한 번에 이동해도 월요일에 맞는다', () => {
    expect(shiftWeek(week, -4)).toEqual({ startDate: '2026-08-17', endDate: '2026-08-23' });
  });
});

describe('todayInKst', () => {
  // UTC 로는 전날이지만 KST 로는 이미 다음 날인 시각. 여기서 하루가 밀리면 "이번 주"가
  // 한 주 전으로 잡힌다.
  it('UTC 자정 직전이어도 KST 날짜를 준다', () => {
    expect(todayInKst(new Date('2026-09-17T15:30:00Z'))).toBe('2026-09-18');
  });

  it('UTC 와 KST 가 같은 날인 시각도 맞게 준다', () => {
    expect(todayInKst(new Date('2026-09-18T03:00:00Z'))).toBe('2026-09-18');
  });
});

describe('currentWeek / isCurrentWeek', () => {
  const now = new Date('2026-09-18T06:00:00Z'); // KST 9/18 15:00 (금)

  it('오늘이 속한 월~일 주를 준다', () => {
    expect(currentWeek(now)).toEqual({ startDate: '2026-09-14', endDate: '2026-09-20' });
  });

  it('이번 주인지 가린다', () => {
    expect(isCurrentWeek({ startDate: '2026-09-14', endDate: '2026-09-20' }, now)).toBe(true);
    expect(isCurrentWeek({ startDate: '2026-09-07', endDate: '2026-09-13' }, now)).toBe(false);
  });
});

describe('isFutureDate', () => {
  const now = new Date('2026-09-18T06:00:00Z'); // KST 9/18 (금)

  it('오늘은 미래가 아니다', () => {
    expect(isFutureDate('2026-09-18', now)).toBe(false);
  });

  it('이번 주의 남은 요일은 미래다', () => {
    expect(isFutureDate('2026-09-19', now)).toBe(true);
    expect(isFutureDate('2026-09-20', now)).toBe(true);
  });

  it('지난 날은 미래가 아니다', () => {
    expect(isFutureDate('2026-09-17', now)).toBe(false);
  });
});

describe('formatWeekLabel', () => {
  it('월/일 - 월/일 로 적는다', () => {
    expect(formatWeekLabel({ startDate: '2026-09-14', endDate: '2026-09-20' })).toBe('9/14 - 9/20');
  });

  it('월을 넘는 주도 양쪽 월을 적는다', () => {
    expect(formatWeekLabel({ startDate: '2026-09-28', endDate: '2026-10-04' })).toBe('9/28 - 10/4');
  });
});
