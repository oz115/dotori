import { BANKS, findBank } from './banks';

describe('banks', () => {
  it('has 5 banks with unique keys and 3-digit codes', () => {
    expect(BANKS).toHaveLength(5);
    const keys = BANKS.map((b) => b.key);
    expect(new Set(keys).size).toBe(5);
    BANKS.forEach((b) => expect(b.code).toMatch(/^\d{3}$/));
  });

  it('findBank resolves a key, or undefined', () => {
    expect(findBank('kb')?.name).toBe('KB국민은행');
    expect(findBank('nope')).toBeUndefined();
    expect(findBank(null)).toBeUndefined();
  });
});
