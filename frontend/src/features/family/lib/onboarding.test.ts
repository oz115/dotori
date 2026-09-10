import { isValidOnboardingAmount, parseAmountInput } from './onboarding';

describe('isValidOnboardingAmount', () => {
  it('accepts 1,000원 이상 · 1,000원 단위', () => {
    expect(isValidOnboardingAmount(1000)).toBe(true);
    expect(isValidOnboardingAmount(5000)).toBe(true);
    expect(isValidOnboardingAmount(21000)).toBe(true);
  });

  it('rejects below minimum, off-unit, null, non-integer', () => {
    expect(isValidOnboardingAmount(0)).toBe(false);
    expect(isValidOnboardingAmount(500)).toBe(false);
    expect(isValidOnboardingAmount(1500)).toBe(false);
    expect(isValidOnboardingAmount(null)).toBe(false);
    expect(isValidOnboardingAmount(1000.5)).toBe(false);
  });
});

describe('parseAmountInput', () => {
  it('strips non-digits', () => {
    expect(parseAmountInput('50,000')).toBe(50000);
    expect(parseAmountInput('5000원')).toBe(5000);
  });

  it('is null when empty', () => {
    expect(parseAmountInput('')).toBeNull();
    expect(parseAmountInput('abc')).toBeNull();
  });
});
