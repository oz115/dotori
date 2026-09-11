/**
 * 가격 추이 배열을 react-native-svg `<Polyline>` 의 points 문자열로 바꾼다.
 * 값 범위를 미니 차트 크기에 맞게 정규화하고, 선 두께가 위아래에서 잘리지 않게
 * inset 만큼 여백을 둔다. 값이 2개 미만이면 선을 그릴 수 없어 빈 문자열을 돌려준다.
 */
export function sparklinePoints(
  values: number[],
  width: number,
  height: number,
  inset = 2,
): string {
  if (values.length < 2) {
    return '';
  }

  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min;
  const innerHeight = height - inset * 2;
  const stepX = width / (values.length - 1);

  return values
    .map((value, index) => {
      // 값이 전부 같으면 가운데 수평선으로 그린다
      const ratio = span === 0 ? 0.5 : (value - min) / span;
      const x = round1(index * stepX);
      const y = round1(inset + (1 - ratio) * innerHeight);
      return `${x},${y}`;
    })
    .join(' ');
}

const round1 = (n: number): number => Math.round(n * 10) / 10;
