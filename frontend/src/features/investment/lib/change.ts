import type { SemanticColor } from '@/shared/theme';

/** 등락은 앱의 투자 색상 규칙을 따르며 0은 상승으로 취급하지 않는다. */
export function changeColor(change: number, colors: SemanticColor): string {
  return change > 0
    ? colors.investment.rise
    : change < 0
      ? colors.investment.fall
      : colors.investment.flat;
}

export function changeLabel(change: number): string {
  return change > 0 ? '상승' : change < 0 ? '하락' : '변동 없음';
}
