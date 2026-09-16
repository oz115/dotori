import { difficultyLabel, isRewardReady, repeatLabel, sumRewards } from './choreLabels';

describe('repeatLabel', () => {
  it('반복 주기를 한글로 옮긴다', () => {
    expect(repeatLabel('ONCE')).toBe('한번');
    expect(repeatLabel('DAY')).toBe('매일');
    expect(repeatLabel('WEEK')).toBe('매주');
    expect(repeatLabel('MONTH')).toBe('매월');
  });
});

describe('difficultyLabel', () => {
  it('난이도를 한글로 옮긴다', () => {
    expect(difficultyLabel('EASY')).toBe('쉬움');
    expect(difficultyLabel('NORMAL')).toBe('보통');
    expect(difficultyLabel('HARD')).toBe('어려움');
  });
});

describe('isRewardReady', () => {
  it('null이면 아직 산정 중이다', () => {
    expect(isRewardReady(null)).toBe(false);
  });

  it('0도 산정된 값으로 본다', () => {
    expect(isRewardReady(0)).toBe(true);
  });
});

describe('sumRewards', () => {
  it('산정된 보상만 더한다', () => {
    expect(sumRewards([500, 300, 200])).toEqual({ total: 1000, pendingCount: 0 });
  });

  it('산정 중인 건은 합계에서 빼고 건수로 센다', () => {
    expect(sumRewards([500, null, 200, null])).toEqual({ total: 700, pendingCount: 2 });
  });

  it('빈 목록이면 0이다', () => {
    expect(sumRewards([])).toEqual({ total: 0, pendingCount: 0 });
  });
});
