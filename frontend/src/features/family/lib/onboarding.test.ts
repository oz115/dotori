import { addAmount, isValidOnboardingAmount, MAX_AMOUNT_WON, parseAmountInput } from './onboarding';

describe('isValidOnboardingAmount', () => {
  it('accepts 1,000원 이상 · 1,000원 단위', () => {
    expect(isValidOnboardingAmount(1000)).toBe(true);
    expect(isValidOnboardingAmount(5000)).toBe(true);
    expect(isValidOnboardingAmount(21000)).toBe(true);
  });

  it('rejects amounts above the cap', () => {
    expect(isValidOnboardingAmount(MAX_AMOUNT_WON)).toBe(true);
    expect(isValidOnboardingAmount(MAX_AMOUNT_WON + 1000)).toBe(false);
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

describe('parseAmountInput - 상한', () => {
  it('상한을 넘는 입력은 상한으로 자른다', () => {
    expect(parseAmountInput('300,000,000')).toBe(MAX_AMOUNT_WON);
    expect(parseAmountInput('99999000000')).toBe(MAX_AMOUNT_WON);
  });

  it('아주 긴 숫자도 상한으로 자른다 (정밀도 손실 방지)', () => {
    expect(parseAmountInput('9'.repeat(30))).toBe(MAX_AMOUNT_WON);
  });

  it('앞의 0 은 무시한다', () => {
    expect(parseAmountInput('005000')).toBe(5000);
  });
});

describe('addAmount', () => {
  it('비어 있으면 0 에서 시작한다', () => {
    expect(addAmount(null, 5000)).toBe(5000);
  });

  it('누른 만큼 더한다', () => {
    expect(addAmount(5000, 10000)).toBe(15000);
  });

  it('상한을 넘으면 상한에서 멈춘다', () => {
    expect(addAmount(MAX_AMOUNT_WON - 1000, 50000)).toBe(MAX_AMOUNT_WON);
  });
});
