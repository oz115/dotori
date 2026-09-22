import { typography } from '@/shared/theme/tokens';

import { scaleTypography, textScaleOf } from './fontScale';

describe('scaleTypography', () => {
  it('배율이 1이면 토큰을 그대로 돌려준다', () => {
    // 대부분의 기기가 여기 해당한다 — 매 렌더 새 객체를 만들어 참조가 바뀌지 않게 한다.
    expect(scaleTypography(1)).toBe(typography);
  });

  it('확대 배율만큼 나눠서 화면에 그려질 크기가 디자인 값이 되게 한다', () => {
    // RN 이 fontSize 에 배율을 다시 곱하므로 18 / 1.5 × 1.5 = 18 이 된다.
    const scaled = scaleTypography(1.5);

    expect(scaled.body.fontSize).toBe(typography.body.fontSize / 1.5);
    expect(scaled.body.lineHeight).toBe(typography.body.lineHeight / 1.5);
  });

  it('축소 배율도 똑같이 되돌린다', () => {
    // 한쪽만 따라가면 기기마다 글자 크기가 달라져 "디자인 크기로 고정"이 아니게 된다.
    const scaled = scaleTypography(0.8);

    expect(scaled.body.fontSize).toBe(typography.body.fontSize / 0.8);
  });

  it('fontFamily 같은 나머지 속성은 건드리지 않는다', () => {
    expect(scaleTypography(1.3).button).toMatchObject({
      fontFamily: typography.button.fontFamily,
      letterSpacing: typography.button.letterSpacing,
    });
  });

  it('모든 토큰을 빠짐없이 보정한다', () => {
    const scaled = scaleTypography(2);

    for (const token of Object.keys(typography) as (keyof typeof typography)[]) {
      expect(scaled[token].fontSize).toBe(typography[token].fontSize / 2);
    }
  });

  it.each([0, -1, NaN])('말이 안 되는 배율(%p)이면 보정하지 않는다', (fontScale) => {
    // 0 으로 나누면 글자가 사라지거나 Infinity 가 된다 — 차라리 확대를 따라간다.
    expect(scaleTypography(fontScale)).toBe(typography);
    expect(textScaleOf(fontScale)).toBe(1);
  });
});

describe('textScaleOf', () => {
  it('직접 적은 크기에 곱하면 배율이 상쇄된다', () => {
    expect(12 * textScaleOf(1.5) * 1.5).toBeCloseTo(12);
  });
});
