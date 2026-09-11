import { formatAcorns, formatNumber, formatPoints, signedAcorns } from './format';

describe('formatPoints', () => {
  it('천 단위 구분 쉼표와 P 접미사를 붙인다', () => {
    expect(formatPoints(7300)).toBe('7,300P');
    expect(formatPoints(1234567)).toBe('1,234,567P');
  });

  it('천 미만은 쉼표 없이 표기한다', () => {
    expect(formatPoints(980)).toBe('980P');
    expect(formatPoints(0)).toBe('0P');
  });
});

describe('formatNumber', () => {
  it('단위 없이 천 단위 구분만 한다', () => {
    expect(formatNumber(1120)).toBe('1,120');
    expect(formatNumber(600)).toBe('600');
  });
});

describe('formatAcorns', () => {
  it('도토리 단위로 표기한다', () => {
    expect(formatAcorns(980)).toBe('980 도토리');
    expect(formatAcorns(1120)).toBe('1,120 도토리');
  });
});

describe('signedAcorns', () => {
  it('오르면 + 부호를 붙인다', () => {
    expect(signedAcorns(20)).toBe('+20 도토리');
    expect(signedAcorns(1500)).toBe('+1,500 도토리');
  });

  it('내리면 - 부호를 붙인다', () => {
    expect(signedAcorns(-15)).toBe('-15 도토리');
  });

  it('변동 없음은 부호 없이 0으로 표기한다', () => {
    expect(signedAcorns(0)).toBe('0 도토리');
  });
});
