import { formatWon, formatWonNumber, formatWonShort } from './money';

describe('formatWon', () => {
  it('groups thousands and appends 원', () => {
    expect(formatWon(5000)).toBe('5,000원');
    expect(formatWon(1234567)).toBe('1,234,567원');
    expect(formatWon(0)).toBe('0원');
  });
});

describe('formatWonNumber', () => {
  it('groups thousands without a unit', () => {
    expect(formatWonNumber(10000)).toBe('10,000');
    expect(formatWonNumber(0)).toBe('0');
  });
});

describe('formatWonShort', () => {
  it('uses 만/천 for round amounts', () => {
    expect(formatWonShort(5000)).toBe('5천 원');
    expect(formatWonShort(10000)).toBe('1만 원');
    expect(formatWonShort(30000)).toBe('3만 원');
    expect(formatWonShort(15000)).toBe('1만 5천 원');
  });

  it('falls back to full notation when not on a 천 boundary', () => {
    expect(formatWonShort(12345)).toBe('12,345원');
    expect(formatWonShort(0)).toBe('0원');
  });
});
