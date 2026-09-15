import { palette } from '@/shared/theme/palette';

/**
 * Semantic 색상 토큰 (light). 화면/컴포넌트는 palette 대신 이 이름을 쓴다.
 * 다크 모드는 현재 미지원 — 추가 시 이 파일과 같은 shape로 dark 매핑을 만들고
 * `theme` 에서 스킴에 따라 고른다.
 */
export const colorLight = {
  background: {
    canvas: palette.neutral[0], // 페이지 기본 배경 (흰색)
    surface: palette.neutral[0], // 카드/시트
    surfaceSunken: palette.neutral[300], // 섹션 구분용 연회색 영역 (#F2F5ED)
    overlay: 'rgba(39, 44, 38, 0.45)', // 모달 딤
  },
  text: {
    strong: palette.neutral[900], // 제목
    default: palette.neutral[700], // 본문
    muted: palette.neutral[400], // 보조 설명
    sub: palette.neutral[500],
    label: palette.neutral[500], // 폼 필드 라벨
    onBrand: palette.neutral[0], // 브랜드 배경 위 글자
    link: palette.green[600],
  },
  border: {
    default: '#E6E6E6', // 인풋/디바이더 hairline
    card: '#F3F4F6', // 흰 콘텐츠 카드 테두리 (그림자와 함께 씀)
    strong: palette.neutral[400],
    focus: palette.green[500], // 인풋 포커스
  },
  brand: {
    solid: palette.green[500], // 기본 CTA 배경
    solidPressed: palette.green[600],
    solidActive: palette.green[700],
    subtle: palette.green[50], // 연한 브랜드 배경 (배지 등)
    tint: 'rgba(233, 246, 229, 0.4)', // green.50 40% — 회원가입 유도 등 은은한 배너
    subtleText: palette.green[700],
    border: palette.green[500],
    black: palette.green[800],
  },
  accent: {
    acorn: palette.acorn,
    cream: palette.cream,
  },
  chip: {
    // 선택되지 않은 필터 칩. Figma 할 일 목록 기준 - 회색 채움 + 흰 글씨라
    // surfaceSunken(거의 흰색)으로는 대체되지 않는다.
    idle: palette.neutral[400],
    idleText: palette.neutral[0],
  },
  status: {
    success: { solid: palette.green[500], subtle: palette.green[50], text: palette.green[700] },
    info: { solid: palette.blue[500], subtle: palette.blue[100], text: palette.blue[700] },
    warning: { solid: palette.amber[500], subtle: palette.amber[100], text: palette.amber[700] },
    danger: { solid: palette.red[500], subtle: palette.red[100], text: palette.red[700] },
  },
} as const;

export type SemanticColor = typeof colorLight;
