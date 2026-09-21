import type { ReportPeriod } from '@/features/report/model/types';

/**
 * 리포트가 보는 기간은 **월요일부터 일요일까지 한 주**다.
 *
 * <p>주 단위로 자른 이유는 화면 때문이다. 한 달을 일 단위로 그리면 막대가 31개, 폰
 * 폭에서 하나에 4px 이라 읽을 수 없다. 7칸이면 하나에 40px 쯤 되고, x축도 요일로 고정된다.
 *
 * <p><b>날짜만 다루고 시각은 다루지 않는다.</b> 서버가 받는 startDate·endDate 가 KST
 * 날짜(양끝 포함)라서다. Date 객체로 요일을 계산할 때 시간대 때문에 하루씩 밀리는 실수가
 * 흔하므로, 모든 계산을 <b>UTC 정오로 고정한 Date</b> 위에서 한다 — 어느 시간대에서
 * 실행해도 같은 날짜가 나온다.
 */

/** YYYY-MM-DD 를 UTC 정오 Date 로. 시간대에 따라 날짜가 밀리는 것을 막는다 */
function atUtcNoon(date: string): Date {
  return new Date(`${date}T12:00:00Z`);
}

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setUTCDate(next.getUTCDate() + days);
  return next;
}

/** 오늘(KST) 의 YYYY-MM-DD */
export function todayInKst(now: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now);
}

/**
 * 그 날짜가 속한 월~일 주.
 *
 * <p>`getUTCDay()` 는 일요일이 0 이라 월요일을 주의 시작으로 쓰려면 하루씩 당겨야 한다
 * (`(day + 6) % 7` 이 월요일까지 되돌릴 일수다).
 */
export function weekOf(date: string): ReportPeriod {
  const d = atUtcNoon(date);
  const monday = addDays(d, -((d.getUTCDay() + 6) % 7));

  return { startDate: toIsoDate(monday), endDate: toIsoDate(addDays(monday, 6)) };
}

/** 이번 주 (오늘이 속한 월~일) */
export function currentWeek(now: Date = new Date()): ReportPeriod {
  return weekOf(todayInKst(now));
}

/** 주 단위로 앞뒤 이동. `-1` 이 지난 주, `+1` 이 다음 주 */
export function shiftWeek(period: ReportPeriod, weeks: number): ReportPeriod {
  return weekOf(toIsoDate(addDays(atUtcNoon(period.startDate), weeks * 7)));
}

/** 그 주의 월~일 일곱 날짜 */
export function daysOfWeek(period: ReportPeriod): string[] {
  const monday = atUtcNoon(period.startDate);
  return Array.from({ length: 7 }, (_, i) => toIsoDate(addDays(monday, i)));
}

/** 화면에 띄울 기간 라벨 — `9/14 - 9/20` */
export function formatWeekLabel(period: ReportPeriod): string {
  const short = (date: string) => {
    const d = atUtcNoon(date);
    return `${d.getUTCMonth() + 1}/${d.getUTCDate()}`;
  };
  return `${short(period.startDate)} - ${short(period.endDate)}`;
}

/** 오늘이 그 주에 들어 있는가 — "이번 주" 표시와 다음 주 이동 차단에 쓴다 */
export function isCurrentWeek(period: ReportPeriod, now: Date = new Date()): boolean {
  return period.startDate === currentWeek(now).startDate;
}

/**
 * 아직 오지 않은 날인가.
 *
 * <p>이번 주를 보면 남은 요일이 있다. 거래가 없어서 0 인 날과 아직 오지 않은 날은
 * 다른 의미라, 화면이 후자를 비워 두기 위해 구분한다.
 */
export function isFutureDate(date: string, now: Date = new Date()): boolean {
  return date > todayInKst(now);
}
