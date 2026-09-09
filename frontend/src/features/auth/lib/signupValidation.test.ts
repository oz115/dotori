import { isValidEmail, validateAccountStep } from './signupValidation';

describe('isValidEmail', () => {
  it.each(['a@b.com', 'parent.01@dotori.town'])('accepts %s', (email) => {
    expect(isValidEmail(email)).toBe(true);
  });

  it.each(['a@b', 'no-at.com', 'a b@c.com', ''])('rejects %s', (email) => {
    expect(isValidEmail(email)).toBe(false);
  });
});

describe('validateAccountStep', () => {
  const valid = {
    loginId: 'parent01',
    password: 'pw12345678',
    passwordConfirm: 'pw12345678',
    name: '영주',
  };

  it('passes with valid input', () => {
    expect(validateAccountStep(valid)).toEqual({});
  });

  it('flags a short id', () => {
    expect(validateAccountStep({ ...valid, loginId: 'ab' }).loginId).toBeDefined();
  });

  it('flags a short password', () => {
    expect(
      validateAccountStep({ ...valid, password: 'short', passwordConfirm: 'short' }).password,
    ).toBeDefined();
  });

  it('flags a password mismatch', () => {
    expect(
      validateAccountStep({ ...valid, passwordConfirm: 'different1' }).passwordConfirm,
    ).toBeDefined();
  });

  it('flags an empty name', () => {
    expect(validateAccountStep({ ...valid, name: '   ' }).name).toBeDefined();
  });
});
