/**
 * 날짜 범위 조회(거래·구매 내역)가 공통으로 쓰는 날짜 유틸.
 * 서버 파라미터/화면 표기 형식과 로컬 자정 기준 변환을 한곳에 모은다 —
 * `new Date('YYYY-MM-DD')`가 UTC 자정으로 해석돼 날짜가 밀리는 함정을 화면마다
 * 다시 밟지 않게 하기 위해서다.
 */

/** Date → "2025-09-07" (서버 startDate/endDate 파라미터용, ISO DATE) */
export function toDateParam(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/**
 * 오늘 날짜 ("2025-09-07"), 서버와 같은 KST 기준.
 *
 * 기기 로컬 시간으로 세면 안 된다 — 시간대가 KST 가 아닌 기기(기본값이 GMT 인 에뮬레이터,
 * 해외)에서는 자정~오전 9시 사이에 날짜가 하루 어긋나, 서버가 "오늘" 로 보는 것과 화면이
 * "오늘" 로 보는 것이 달라진다. 하루 1회 제한·당일 지급 조회처럼 날짜 경계가 규칙인
 * 곳에서 그 한 칸이 곧 오동작이다.
 */
export function todayDateKst(now: number = Date.now()): string {
  // KST 는 서머타임이 없어 +9시간을 더한 UTC 날짜가 곧 KST 날짜다.
  return new Date(now + KST_OFFSET_MS).toISOString().slice(0, 10);
}

const KST_OFFSET_MS = 9 * 60 * 60 * 1000;

/** "2025-09-07" → 로컬 자정 Date */
export function fromDateParam(dateString: string): Date {
  const [year, month, day] = dateString.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** Date → "2025.09.07" (화면 표시용) */
export function formatDateShort(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())}`;
}

export function addMonths(date: Date, n: number): Date {
  const d = new Date(date);
  d.setMonth(d.getMonth() + n);
  return d;
}

export function addDays(date: Date, n: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + n);
  return d;
}

export function monthsAgo(n: number, from = new Date()): Date {
  return addMonths(from, -n);
}

export function minDate(a: Date, b: Date): Date {
  return a < b ? a : b;
}

/** ISO datetime 이 "YYYY-MM-DD" 범위(양 끝 포함) 안의 날짜인지. ISO 는 문자열 비교로 충분하다 */
export function isoWithinRange(
  iso: string,
  range: { startDate: string; endDate: string },
): boolean {
  const date = iso.slice(0, 10);
  return date >= range.startDate && date <= range.endDate;
}
