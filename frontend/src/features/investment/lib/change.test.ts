import { theme } from '@/shared/theme';
import { changeColor, changeLabel, sinceBoughtLabel } from './change';

it('상승 파랑·하락 빨강·보합 중립색과 문구를 구분한다', () => {
  expect(changeColor(1, theme.color)).toBe(theme.color.investment.rise);
  expect(changeColor(-1, theme.color)).toBe(theme.color.investment.fall);
  expect(changeColor(0, theme.color)).toBe(theme.color.investment.flat);
  expect(changeLabel(0)).toBe('변동 없음');
});

it('산 값 대비 변화를 올랐어요/내렸어요/같아요로 말한다', () => {
  expect(sinceBoughtLabel(300)).toBe('산 값보다 300 도토리 올랐어요');
  expect(sinceBoughtLabel(-1200)).toBe('산 값보다 1,200 도토리 내렸어요');
  expect(sinceBoughtLabel(0)).toBe('산 값과 같아요');
});
