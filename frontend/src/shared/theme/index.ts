import { useMemo } from 'react';
import { useWindowDimensions, type TextStyle } from 'react-native';

import { scaleTypography, textScaleOf } from '@/shared/theme/fontScale';
import { palette } from '@/shared/theme/palette';
import { colorLight } from '@/shared/theme/semantic';
import {
  layout,
  radius,
  shadow,
  space,
  typography,
  type TypographyToken,
} from '@/shared/theme/tokens';

/**
 * shared/theme public API.
 *
 * 화면/컴포넌트는 `useTheme()`로 토큰을 가져다 쓴다:
 *
 *   const theme = useTheme();
 *   <View style={{ backgroundColor: theme.color.background.canvas, padding: theme.space.lg }} />
 *   <Text style={theme.typography.h2} />
 *
 * 아래 `theme` 는 디자인 값 그대로의 고정 객체다(테스트·상수 참조용). 화면은 `useTheme()`
 * 를 쓴다 — 기기 글꼴 배율을 상쇄한 타이포그래피를 돌려준다.
 *
 * 현재 색은 라이트 전용이다. 다크 모드가 생기면 `useTheme()`가 컬러 스킴에 따라 다른
 * semantic 매핑을 반환하도록 바꾼다.
 */
export const theme = {
  palette,
  color: colorLight,
  space,
  radius,
  shadow,
  typography,
  layout,
} as const;

/**
 * 타이포그래피는 기기 글꼴 배율에 따라 값이 달라지므로 리터럴이 아니라 TextStyle 로 넓힌다.
 * textScale 도 마찬가지 — 토큰 정의는 고정값이지만 useTheme 이 돌려주는 것은 계산된 값이다.
 */
export type Theme = Omit<typeof theme, 'typography'> & {
  typography: Record<TypographyToken, TextStyle>;
  /** 직접 적은 글자 크기를 보정할 때 곱하는 값 (fontScale.ts 참고). 확대가 없으면 1 */
  textScale: number;
};

/**
 * 화면·컴포넌트가 쓰는 테마.
 *
 * 색·간격은 고정이고 타이포그래피만 기기 설정에 따라 달라진다 — 시스템 글꼴 확대를 상쇄해
 * 항상 디자인 크기로 그린다 (이유는 shared/theme/fontScale.ts).
 */
export function useTheme(): Theme {
  const { fontScale } = useWindowDimensions();
  return useMemo(
    () => ({ ...theme, typography: scaleTypography(fontScale), textScale: textScaleOf(fontScale) }),
    [fontScale],
  );
}

export { fontFamily, useAppFonts } from '@/shared/theme/fonts';
export { bankBrand, type BankKey } from '@/shared/theme/bankColors';
export type { Space, Radius, TypographyToken } from '@/shared/theme/tokens';
export type { SemanticColor } from '@/shared/theme/semantic';
export type { Palette } from '@/shared/theme/palette';
