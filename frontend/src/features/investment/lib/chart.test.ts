import { axisTicks, scalePoints, scaleY, toPolylinePoints } from './chart';

describe('axisTicks', () => {
  it('값 범위를 덮는 보기 좋은 간격의 눈금을 만든다 (Figma 시안 케이스)', () => {
    expect(axisTicks(650, 1120)).toEqual([600, 800, 1000, 1200]);
  });

  it('작은 범위도 눈금이 범위를 덮는다', () => {
    expect(axisTicks(0, 10)).toEqual([0, 5, 10]);
  });

  it('값이 하나뿐이면 범위를 벌려서라도 눈금을 만든다', () => {
    const ticks = axisTicks(1000, 1000);
    expect(ticks.length).toBeGreaterThanOrEqual(2);
    expect(ticks[0]).toBeLessThanOrEqual(1000);
    expect(ticks[ticks.length - 1]).toBeGreaterThanOrEqual(1000);
  });
});

describe('scaleY', () => {
  it('domain 최솟값은 아래쪽, 최댓값은 위쪽 inset 경계에 놓인다', () => {
    expect(scaleY(600, 600, 1200, 100, 5)).toBe(95);
    expect(scaleY(1200, 600, 1200, 100, 5)).toBe(5);
  });

  it('범위가 0이면 가운데에 놓인다', () => {
    expect(scaleY(50, 50, 50, 100, 0)).toBe(50);
  });
});

describe('scalePoints', () => {
  it('값이 없으면 빈 배열을 돌려준다', () => {
    expect(scalePoints([], 0, 100, 100, 100)).toEqual([]);
  });

  it('값이 하나뿐이면 선을 그릴 기준이 없어 오른쪽 끝에 점 하나만 찍는다', () => {
    expect(scalePoints([50], 0, 100, 90, 50, 0)).toEqual([{ x: 90, y: 25 }]);
  });

  it('x 는 균등 간격, y 는 domain 기준으로 정규화된다', () => {
    const points = scalePoints([0, 100], 0, 100, 90, 50, 0);
    expect(points).toEqual([
      { x: 0, y: 50 },
      { x: 90, y: 0 },
    ]);
  });

  it('inset 은 좌우에도 적용돼 양끝 점이 잘리지 않는다', () => {
    const points = scalePoints([0, 100], 0, 100, 100, 50, 5);
    expect(points[0].x).toBe(5);
    expect(points[points.length - 1].x).toBe(95);
    expect(scalePoints([50], 0, 100, 100, 50, 5)[0].x).toBe(95);
  });
});

describe('toPolylinePoints', () => {
  it('SVG points 문자열로 변환한다', () => {
    expect(
      toPolylinePoints([
        { x: 0, y: 1.5 },
        { x: 10, y: 2 },
      ]),
    ).toBe('0,1.5 10,2');
  });
});

it('보합 날짜는 같은 높이로 이어지고 상승한 날만 높이가 바뀐다', () => {
  const points = scalePoints([100, 100, 100, 110, 110], 100, 110, 400, 100);
  expect(points.map((p) => p.x)).toEqual([0, 100, 200, 300, 400]);
  expect(points.map((p) => p.y)).toEqual([100, 100, 100, 0, 0]);
});
