/**
 * 서버가 준 시각 문자열을 Date 로 바꾸는 공용 파서.
 *
 * 서버는 `OffsetDateTime` 을 그대로 직렬화해서 소수점 초가 **6자리(마이크로초)**로 온다
 * (`2026-10-16T06:00:57.162979Z`). 그런데 ES 명세의 Date Time String Format 은 소수점
 * 초를 정확히 3자리로 정의하고, **Hermes 는 그 규격을 엄격하게 지켜 6자리를 주면
 * `Invalid Date` 를 돌려준다.** V8(웹·Node)은 관대해서 그냥 파싱하므로, 테스트는 통과하는데
 * 실기기에서만 날짜가 사라지는 형태로 드러난다 — 잡기 어려운 부류다.
 *
 * 그래서 `new Date(iso)` 를 화면에서 직접 부르지 않고 여기를 거친다.
 */

/** 소수점 초를 3자리로 자른다. 3자리 이하나 소수점이 없는 문자열은 그대로 둔다. */
export function normalizeFractionalSeconds(iso: string): string {
  return iso.replace(/(\.\d{3})\d+/, '$1');
}

/**
 * 서버 시각 문자열 → Date. 파싱할 수 없으면 null 이다.
 *
 * <p>null 을 그대로 통과시키므로 "만기 없는 상품"처럼 값이 없는 경우를 호출부가
 * 따로 분기하지 않아도 된다.
 */
export function parseServerDate(iso: string | null | undefined): Date | null {
  if (iso == null) {
    return null;
  }

  const date = new Date(normalizeFractionalSeconds(iso));
  return Number.isNaN(date.getTime()) ? null : date;
}
