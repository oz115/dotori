/**
 * Primitive 색상 토큰. Figma "color palette" 프레임에서 그대로 옮긴 값이며,
 * 화면 코드에서 직접 쓰지 말고 semantic 토큰(`color`)을 통해 사용합니다.
 * (Figma: A505 / node 58:102)
 */
export const palette = {
  // Key color + 밝기 스케일 (light → dark)
  green: {
    50: '#E9F6E5',
    100: '#D4EDC6',
    200: '#C2E4AF',
    300: '#B3DB9C',
    400: '#8AC16F',
    500: '#3DAD39', // key / primary
    600: '#3E983A', // pressed
    700: '#2F782D', // active / deep
  },
  greenMuted: '#619D46', // key color 2 (톤 다운된 보조 그린)

  // 그린 그라디언트 (2-stop). LinearGradient의 colors 배열로 사용.
  gradient: {
    fresh: ['#68C380', '#8FD14F'] as const,
    deep: ['#81BC53', '#4FA042'] as const,
    light: ['#5E9C44', '#ACE874'] as const,
  },

  neutral: {
    0: '#FFFFFF',
    50: '#F8FAF4', // 가장 밝은 그린빛 화이트
    100: '#F6F8F1',
    200: '#F2F5ED', // 페이지 배경
    300: '#F9F9F9', // 회색 배경
    400: '#B1B6B1', // subtitle / muted
    500: '#686968',
    700: '#49504A', // 본문
    900: '#272C26', // 제목 / 강조 (그린빛 블랙)
  },

  // 도토리 세계관 액센트
  acorn: '#825430', // 도토리 브라운
  cream: '#FFFCF8', // 따뜻한 화이트

  // 시스템 상태색 — Figma 팔레트에 없어 합의된 기본값으로 채움 (디자인 확정 시 교체)
  red: { 500: '#E5484D', 100: '#FDECEC', 700: '#B32B30' },
  amber: { 500: '#F2A73B', 100: '#FEF3E2', 700: '#9A6212' },
  blue: { 500: '#2F80ED', 100: '#E6F0FD', 700: '#1B5FB8' },
} as const;

export type Palette = typeof palette;
