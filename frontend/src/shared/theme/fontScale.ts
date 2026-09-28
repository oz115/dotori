import type { TextStyle } from 'react-native';

import { typography } from '@/shared/theme/tokens';

/**
 * 시스템 글꼴 설정을 상쇄해 앱을 어느 기기에서나 같은 크기로 그린다.
 *
 * <p>안드로이드 설정에서 글자 크기를 키우면 RN 이 모든 `fontSize`·`lineHeight` 에 그 배율을
 * 곱한다. 삼성 기준 4단계만 넘어가도 카드 안에서 글이 일러스트를 덮어써서 읽을 수 없게
 * 됐다 (QA). 그래서 기기 설정을 따라가지 않기로 했다 — 우리가 선언한 값을 기기 배율로
 * 나눠 두면 RN 이 다시 곱했을 때 우리가 정한 크기가 된다.
 *
 * <p>`<Text allowFontScaling={false}>` 과 같은 방식인데, 그 prop 은 Text 마다 붙여야 한다.
 * RN 0.86 + React 19 에서는 `Text.defaultProps` 가 없어져 한 번에 기본값을 바꿀 수 없어서,
 * 모든 화면이 거쳐 가는 테마에서 처리한다.
 *
 * <p>줄이는 방향(배율 &lt; 1)도 똑같이 되돌린다. 한쪽만 따라가면 기기마다 글자 크기가
 * 달라져 고정이 아니게 된다.
 *
 * <p>한계: `StyleSheet.create` 에 직접 적은 `fontSize` 는 테마를 거치지 않아 기기 설정을
 * 그대로 따라간다. 새 글자 크기는 타이포그래피 토큰(`theme.typography.*`)을 쓰거나,
 * 꼭 직접 적어야 하면 `theme.textScale` 을 곱한다.
 */

/**
 * 앱이 항상 그릴 크기 — 디자인 토큰의 0.9배.
 *
 * <p>기기 글꼴 2단계(배율 0.9)에서 보던 크기가 팀이 쓰던 기준이었다. 기기 설정을 끊으면서
 * 1.0 으로 두면 쓰던 것보다 글자가 커져 버려서, 고정값 자체를 그 크기로 맞춘다.
 */
export const FIXED_FONT_SCALE = 0.9;

export type Typography = typeof typography;

/** 보정할 것이 없을 때(기기 배율이 마침 고정값과 같을 때) 새 객체를 만들지 않는다. */
export function scaleTypography(fontScale: number): Typography {
  const factor = textScaleOf(fontScale);
  if (factor === 1) {
    return typography;
  }

  const entries = Object.entries(typography).map(([token, style]) => [
    token,
    scaleTextStyle(style, factor),
  ]);
  return Object.fromEntries(entries) as Typography;
}

/**
 * 직접 적은 글자 크기를 보정할 때 곱하는 값 (= 고정 배율 / 시스템 배율).
 * 토큰으로 못 옮기는 자리(차트 축 라벨, 이모지 일러스트 등)에서만 쓴다.
 */
export function textScaleOf(fontScale: number): number {
  return isUsableScale(fontScale) ? FIXED_FONT_SCALE / fontScale : 1;
}

function scaleTextStyle(style: TextStyle, factor: number): TextStyle {
  const scaled: TextStyle = { ...style };
  if (typeof style.fontSize === 'number') {
    scaled.fontSize = style.fontSize * factor;
  }
  if (typeof style.lineHeight === 'number') {
    scaled.lineHeight = style.lineHeight * factor;
  }
  return scaled;
}

/** 0·음수·NaN 이 오면 나누기가 글자를 없애버린다 — 그럴 땐 보정하지 않는다. */
function isUsableScale(fontScale: number): boolean {
  return Number.isFinite(fontScale) && fontScale > 0;
}
