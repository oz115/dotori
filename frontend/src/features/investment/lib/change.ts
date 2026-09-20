import { formatAcorns } from '@/features/investment/lib/format';
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

/** 산 값 대비 지금 얼마나 올랐는지/내렸는지 아이 말투 한 줄. 요약 카드에서 쓴다. */
export function sinceBoughtLabel(change: number): string {
  if (change === 0) return '산 값과 같아요';
  return `산 값보다 ${formatAcorns(Math.abs(change))} ${change > 0 ? '올랐어요' : '내렸어요'}`;
}
