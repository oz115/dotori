import { daysUntilMaturity } from './summary';

describe('daysUntilMaturity', () => {
  const now = new Date(2026, 8, 17, 15, 0);

  it('남은 날을 날짜 경계로 센다', () => {
    expect(daysUntilMaturity(new Date(2026, 8, 24, 1, 0).toISOString(), now)).toBe(7);
  });

  it('오늘이 만기면 0이다', () => {
    expect(daysUntilMaturity(new Date(2026, 8, 17, 23, 0).toISOString(), now)).toBe(0);
  });

  // 만기가 지난 건은 수령 대기 건수가 따로 알려 주므로 "D-음수"를 보여 줄 이유가 없다.
  it('이미 지난 만기는 null 이다', () => {
    expect(daysUntilMaturity(new Date(2026, 8, 16, 9, 0).toISOString(), now)).toBeNull();
  });

  it('만기가 없는 상품(보통예금)은 null 이다', () => {
    expect(daysUntilMaturity(null, now)).toBeNull();
  });
});
