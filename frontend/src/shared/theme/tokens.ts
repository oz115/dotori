import type { TextStyle } from 'react-native';

import { fontFamily } from '@/shared/theme/fonts';

/**
 * 레이아웃 primitive 토큰 (색 제외). 화면에서 raw 숫자를 반복하지 말고 여기를 통해 쓴다.
 */

/** 간격 스케일 (4의 배수). margin/padding/gap 공통. */
export const space = {
  none: 0,
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  '2xl': 24,
  '3xl': 32,
  '4xl': 40,
  '5xl': 48,
} as const;

/** 모서리 둥글기. Figma: 인풋=none(밑줄), 버튼≈md, 카드/토스트≈xl, pill 버튼=full. */
export const radius = {
  none: 0,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  '2xl': 28,
  full: 999,
} as const;

/**
 * 타이포그래피 프리셋. `<Text style={theme.typography.body}>` 형태로 사용.
 * Figma 시안이 약 1.9배 스케일이라 값은 실제 기기 기준으로 환산했다.
 */
export const typography = {
  display: {
    fontFamily: fontFamily.bold,
    fontSize: 34,
    lineHeight: 42,
  },
  h1: {
    fontFamily: fontFamily.bold,
    fontSize: 28,
    lineHeight: 36,
  },
  h2: {
    fontFamily: fontFamily.semibold,
    fontSize: 24,
    lineHeight: 32,
  },
  h3: {
    fontFamily: fontFamily.semibold,
    fontSize: 20,
    lineHeight: 28,
  },
  titleMd: {
    fontFamily: fontFamily.semibold,
    fontSize: 18,
    lineHeight: 26,
  },
  body: {
    fontFamily: fontFamily.regular,
    fontSize: 16,
    lineHeight: 24,
  },
  bodySm: {
    fontFamily: fontFamily.regular,
    fontSize: 14,
    lineHeight: 20,
  },
  label: {
    fontFamily: fontFamily.medium,
    fontSize: 14,
    lineHeight: 18,
  },
  button: {
    fontFamily: fontFamily.semibold,
    fontSize: 16,
    lineHeight: 20,
    letterSpacing: 0.3,
  },
  caption: {
    fontFamily: fontFamily.regular,
    fontSize: 12,
    lineHeight: 16,
  },
} as const satisfies Record<string, TextStyle>;

export type Space = keyof typeof space;
export type Radius = keyof typeof radius;
export type TypographyToken = keyof typeof typography;
