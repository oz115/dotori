import { sparklinePoints } from './sparkline';

describe('sparklinePoints', () => {
  it('값이 2개 미만이면 빈 문자열을 돌려준다', () => {
    expect(sparklinePoints([], 64, 28)).toBe('');
    expect(sparklinePoints([100], 64, 28)).toBe('');
  });

  it('최솟값은 아래쪽, 최댓값은 위쪽 inset 경계에 놓인다', () => {
    // width 60, height 24, inset 2 → y 는 2(최대) ~ 22(최소) 범위
    const points = sparklinePoints([100, 200], 60, 24, 2);
    expect(points).toBe('0,22 60,2');
  });

  it('값이 전부 같으면 가운데 수평선으로 그린다', () => {
    const points = sparklinePoints([50, 50, 50], 60, 24, 2);
    // innerHeight 20 의 절반 + inset 2 = 12
    expect(points).toBe('0,12 30,12 60,12');
  });

  it('값 개수만큼 좌표 쌍을 만들고 x 는 균등 간격이다', () => {
    const points = sparklinePoints([1, 3, 2, 4], 90, 28);
    const pairs = points.split(' ');
    expect(pairs).toHaveLength(4);
    expect(pairs.map((p) => p.split(',')[0])).toEqual(['0', '30', '60', '90']);
  });
});
