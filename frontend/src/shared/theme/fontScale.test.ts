import { typography } from '@/shared/theme/tokens';

import { FIXED_FONT_SCALE, scaleTypography, textScaleOf } from './fontScale';

/** 실제로 화면에 그려지는 크기 = 우리가 선언한 값 × 기기 배율 */
const rendered = (declared: number, fontScale: number) => declared * fontScale;

describe('scaleTypography', () => {
  it('기기 배율이 마침 고정값과 같으면 토큰을 그대로 돌려준다', () => {
    // 매 렌더 새 객체를 만들어 참조가 바뀌지 않게 한다.
    expect(scaleTypography(FIXED_FONT_SCALE)).toBe(typography);
  });

  it.each([0.8, 1, 1.3, 1.8])(
    '기기 배율이 %p 여도 화면에 그려지는 크기는 항상 같다',
    (fontScale) => {
      const scaled = scaleTypography(fontScale);

      expect(rendered(scaled.body.fontSize as number, fontScale)).toBeCloseTo(
        typography.body.fontSize * FIXED_FONT_SCALE,
      );
      expect(rendered(scaled.body.lineHeight as number, fontScale)).toBeCloseTo(
        typography.body.lineHeight * FIXED_FONT_SCALE,
      );
    },
  );

  it('fontFamily 같은 나머지 속성은 건드리지 않는다', () => {
    expect(scaleTypography(1.3).button).toMatchObject({
      fontFamily: typography.button.fontFamily,
      letterSpacing: typography.button.letterSpacing,
    });
  });

  it('모든 토큰을 빠짐없이 보정한다', () => {
    const scaled = scaleTypography(2);

    for (const token of Object.keys(typography) as (keyof typeof typography)[]) {
      expect(rendered(scaled[token].fontSize as number, 2)).toBeCloseTo(
        typography[token].fontSize * FIXED_FONT_SCALE,
      );
    }
  });

  it.each([0, -1, NaN])('말이 안 되는 배율(%p)이면 보정하지 않는다', (fontScale) => {
    // 0 으로 나누면 글자가 사라지거나 Infinity 가 된다 — 차라리 기기 설정을 따라간다.
    expect(scaleTypography(fontScale)).toBe(typography);
    expect(textScaleOf(fontScale)).toBe(1);
  });
});

describe('textScaleOf', () => {
  it('직접 적은 크기에 곱하면 기기 배율과 무관하게 같은 크기가 된다', () => {
    expect(rendered(12 * textScaleOf(1.5), 1.5)).toBeCloseTo(12 * FIXED_FONT_SCALE);
    expect(rendered(12 * textScaleOf(0.8), 0.8)).toBeCloseTo(12 * FIXED_FONT_SCALE);
  });
});
