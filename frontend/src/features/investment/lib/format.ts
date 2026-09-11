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

export function formatAcorns(amount: number): string {
  return `${thousands(amount)} 도토리`;
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
