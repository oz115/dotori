import type { TextStyle } from 'react-native';

import { typography } from '@/shared/theme/tokens';

/**
 * 시스템 글꼴 확대를 상쇄해 앱을 항상 디자인 크기로 그린다.
 *
 * <p>안드로이드 설정에서 글자 크기를 키우면 RN 이 모든 `fontSize`·`lineHeight` 에 그 배율을
 * 곱한다. 삼성 기준 4단계만 넘어가도 카드 안에서 글이 일러스트를 덮어써서 읽을 수 없게
 * 됐다 (QA). 그래서 확대를 따라가지 않기로 했다 — 대신 우리가 선언한 값을 배율로 나눠 두면
 * RN 이 다시 곱했을 때 원래 크기가 된다.
 *
 * <p>`<Text allowFontScaling={false}>` 과 같은 결과인데, 그 prop 은 Text 마다 붙여야 한다.
 * RN 0.86 + React 19 에서는 `Text.defaultProps` 가 없어져 한 번에 기본값을 바꿀 수 없어서,
 * 모든 화면이 거쳐 가는 테마에서 처리한다.
 *
 * <p>줄이는 방향(배율 &lt; 1)도 똑같이 되돌린다. 한쪽만 따라가면 기기마다 글자 크기가 달라져
 * "디자인 크기로 고정"이 아니게 된다.
 *
 * <p>한계: `StyleSheet.create` 에 직접 적은 `fontSize` 는 테마를 거치지 않아 그대로 확대된다.
 * 새 글자 크기는 타이포그래피 토큰(`theme.typography.*`)을 쓰거나, 꼭 직접 적어야 하면
 * `theme.textScale` 을 곱한다.
 */
export type Typography = typeof typography;

/** 배율이 1 일 때(대부분의 기기) 새 객체를 만들지 않도록 원본을 그대로 돌려준다. */
export function scaleTypography(fontScale: number): Typography {
  if (!isUsableScale(fontScale) || fontScale === 1) {
    return typography;
  }

  const entries = Object.entries(typography).map(([token, style]) => [
    token,
    scaleTextStyle(style, 1 / fontScale),
  ]);
  return Object.fromEntries(entries) as Typography;
}

/**
 * 직접 적은 글자 크기를 보정할 때 곱하는 값 (= 1 / 시스템 배율).
 * 토큰으로 못 옮기는 자리(차트 축 라벨, 이모지 일러스트 등)에서만 쓴다.
 */
export function textScaleOf(fontScale: number): number {
  return isUsableScale(fontScale) ? 1 / fontScale : 1;
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
