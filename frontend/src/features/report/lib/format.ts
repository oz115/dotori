/**
 * 리포트 화면의 금액 표기.
 *
 * <p>투자 쪽 `signedPoints` 와 같은 모양이지만 그것을 재사용하지 않는다 — features 끼리
 * 직접 참조하면 FSD 레이어 규칙을 벗어난다(steiger 가 잡는다). 세 번째 feature 가 같은
 * 표기를 필요로 하게 되면 그때 shared 로 올리는 것이 맞다.
 */

/** 부호 있는 포인트 표기 — `+500P` / `-200P` / `0P` */
export function signedPoint(value: number): string {
  if (value === 0) {
    return '0P';
  }
  const sign = value > 0 ? '+' : '-';
  return `${sign}${Math.abs(value).toLocaleString('ko-KR')}P`;
}
