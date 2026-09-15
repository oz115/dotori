import { newIdempotencyKey } from './idempotencyKey';

describe('newIdempotencyKey', () => {
  it('returns non-empty strings that differ across calls', () => {
    const a = newIdempotencyKey();
    const b = newIdempotencyKey();

    expect(a).toBeTruthy();
    expect(b).toBeTruthy();
    expect(a).not.toBe(b);
  });
});
