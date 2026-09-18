/**
 * 상세 화면 가격 차트의 좌표 계산. 렌더링(react-native-svg)과 분리한 순수 함수라
 * 단위 테스트로 덮는다. 미니 차트(sparkline.ts)와 달리 축 눈금이 있어서,
 * 선을 데이터 범위가 아니라 눈금 범위(domain)에 맞춰 그려야 그리드와 어긋나지 않는다.
 */

export type ChartPoint = { x: number; y: number };

/**
 * 값 범위를 덮는 "보기 좋은" 간격(1·2·2.5·5 × 10^n)의 축 눈금.
 * 예: 650~1120 → [600, 800, 1000, 1200]
 */
export function axisTicks(min: number, max: number, targetCount = 4): number[] {
  if (min === max) {
    // 값이 하나뿐이면 범위를 살짝 벌려 수평선이 가운데 오게 한다
    return axisTicks(min - 1, max + 1, targetCount);
  }
  const rawStep = (max - min) / (targetCount - 1);
  const magnitude = 10 ** Math.floor(Math.log10(rawStep));
  const step =
    [1, 2, 2.5, 5, 10].map((multiplier) => multiplier * magnitude).find((c) => c >= rawStep) ??
    10 * magnitude;

  const ticks: number[] = [];
  let tick = Math.floor(min / step) * step;
  for (;;) {
    ticks.push(round1(tick));
    if (tick >= max) {
      return ticks;
    }
    tick += step;
  }
}

/**
 * 값 배열을 차트 영역 좌표로 정규화한다. domain 은 축 눈금의 양 끝을 넘기면 된다.
 * inset 은 선 두께·마지막 점이 위아래에서 잘리지 않게 하는 여백.
 */
export function scalePoints(
  values: number[],
  domainMin: number,
  domainMax: number,
  width: number,
  height: number,
  inset = 0,
): ChartPoint[] {
  if (values.length === 0) {
    return [];
  }
  if (values.length === 1) {
    // 점이 하나뿐이면 선을 그릴 기준(step)이 없다 — 마지막 값이 늘 오른쪽 끝에 오는
    // 다중 포인트 배치와 맞춰, 오른쪽 끝에 점 하나만 찍는다 (호출 쪽이 점 하나일 땐
    // 선·영역 없이 점만 그린다).
    return [{ x: round1(width), y: scaleY(values[0], domainMin, domainMax, height, inset) }];
  }
  const stepX = width / (values.length - 1);
  return values.map((value, index) => ({
    x: round1(index * stepX),
    y: scaleY(value, domainMin, domainMax, height, inset),
  }));
}

/** 값 하나를 y 좌표로 바꾼다 (domainMax 가 위쪽). 그리드 선 위치 계산에도 쓴다. */
export function scaleY(
  value: number,
  domainMin: number,
  domainMax: number,
  height: number,
  inset = 0,
): number {
  const span = domainMax - domainMin;
  const ratio = span === 0 ? 0.5 : (value - domainMin) / span;
  return round1(inset + (1 - ratio) * (height - inset * 2));
}

export function toPolylinePoints(points: ChartPoint[]): string {
  return points.map((point) => `${point.x},${point.y}`).join(' ');
}

const round1 = (n: number): number => Math.round(n * 10) / 10;
