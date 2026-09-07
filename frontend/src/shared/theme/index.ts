import { palette } from '@/shared/theme/palette';
import { colorLight } from '@/shared/theme/semantic';
import { radius, space, typography } from '@/shared/theme/tokens';

/**
 * shared/theme public API.
 *
 * 화면/컴포넌트는 `useTheme()`로 토큰을 가져다 쓴다:
 *
 *   const theme = useTheme();
 *   <View style={{ backgroundColor: theme.color.background.canvas, padding: theme.space.lg }} />
 *   <Text style={theme.typography.h2} />
 *
 * 현재 라이트 전용이라 `theme`는 고정 객체다. 다크 모드가 생기면
 * `useTheme()`가 컬러 스킴에 따라 다른 semantic 매핑을 반환하도록 바꾼다.
 */
export const theme = {
  palette,
  color: colorLight,
  space,
  radius,
  typography,
} as const;

export type Theme = typeof theme;

export function useTheme(): Theme {
  return theme;
}

export { fontFamily, useAppFonts } from '@/shared/theme/fonts';
export { bankBrand, type BankKey } from '@/shared/theme/bankColors';
export type { Space, Radius, TypographyToken } from '@/shared/theme/tokens';
export type { SemanticColor } from '@/shared/theme/semantic';
export type { Palette } from '@/shared/theme/palette';
