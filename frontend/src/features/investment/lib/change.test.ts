import { theme } from '@/shared/theme';
import { changeColor, changeLabel } from './change';

it('상승 파랑·하락 빨강·보합 중립색과 문구를 구분한다', () => {
  expect(changeColor(1, theme.color)).toBe(theme.color.investment.rise);
  expect(changeColor(-1, theme.color)).toBe(theme.color.investment.fall);
  expect(changeColor(0, theme.color)).toBe(theme.color.investment.flat);
  expect(changeLabel(0)).toBe('변동 없음');
});
