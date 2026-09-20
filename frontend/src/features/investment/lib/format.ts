/**
 * 투자 화면의 금액 표기. 포인트는 `7,300P`, 주가는 `980 도토리`,
 * 전일 대비 등락은 `+20 도토리`/`-15 도토리` 형태로 통일한다.
 * (Hermes 의 Intl 지원 차이를 타지 않도록 천 단위 구분은 직접 찍는다)
 */

const thousands = (value: number): string =>
  Math.trunc(Math.abs(value))
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, ',');

export function formatPoints(points: number): string {
  return `${thousands(points)}P`;
}

/** 단위 없이 천 단위 구분만 (차트 축·현재가 큰 숫자처럼 단위를 따로 그리는 자리) */
export function formatNumber(value: number): string {
  return thousands(value);
}

export function formatAcorns(amount: number): string {
  return `${thousands(amount)} 도토리`;
}

/** 부호 있는 포인트 표기 — `+500P`/`-200P`. 좁은 타일에서 "도토리" 단위가 안 들어갈 때 쓴다 */
export function signedPoints(change: number): string {
  if (change > 0) {
    return `+${thousands(change)}P`;
  }
  if (change < 0) {
    return `-${thousands(change)}P`;
  }
  return '0P';
}

export function signedAcorns(change: number): string {
  if (change > 0) {
    return `+${thousands(change)} 도토리`;
  }
  if (change < 0) {
    return `-${thousands(change)} 도토리`;
  }
  return '0 도토리';
}

/** 실현 손익은 매도 대금과 구분해서 안내한다. */
export function profitLabel(profit: number): string {
  return profit > 0
    ? '수익 ' + formatAcorns(profit)
    : profit < 0
      ? '손해 ' + formatAcorns(-profit)
      : '수익 0 도토리';
}
