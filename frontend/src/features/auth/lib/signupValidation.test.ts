import {
  isValidEmail,
  LOGIN_ID_BLANK_ERROR,
  LOGIN_ID_MAX,
  validateAccountStep,
  validateLoginId,
} from './signupValidation';

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

  it('flags an id longer than the server limit with a clear message', () => {
    const tooLong = 'a'.repeat(LOGIN_ID_MAX + 1);
    expect(validateAccountStep({ ...valid, loginId: tooLong }).loginId).toBe(
      '아이디는 50자 이하로 입력해주세요.',
    );
  });

  it('공백이 섞인 아이디를 막는다', () => {
    expect(validateAccountStep({ ...valid, loginId: 'hong gil dong' }).loginId).toBe(
      LOGIN_ID_BLANK_ERROR,
    );
  });

  it('accepts an id of exactly the limit', () => {
    expect(
      validateAccountStep({ ...valid, loginId: 'a'.repeat(LOGIN_ID_MAX) }).loginId,
    ).toBeUndefined();
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

describe('validateLoginId', () => {
  it('길이가 범위(4~50자) 안이면 undefined', () => {
    expect(validateLoginId('dotori')).toBeUndefined();
  });

  it.each(['hong gil dong', ' dotori', 'dotori ', 'do\ttori'])(
    '공백이 섞인 %p 는 막는다',
    (loginId) => {
      // 길이만 보던 때는 이런 아이디가 그대로 가입됐다. 서버 제약도 길이뿐이라
      // 아무도 막지 않았고, 로그인할 때 보이지 않는 한 칸 때문에 아이디가 틀렸다 (QA).
      expect(validateLoginId(loginId)).toBe(LOGIN_ID_BLANK_ERROR);
    },
  );

  it('공백은 길이보다 먼저 알린다 — 더 입력해도 해결되지 않는 문제라서', () => {
    expect(validateLoginId('  ab  ')).toBe(LOGIN_ID_BLANK_ERROR);
  });

  it('공백이 없으면 길이 검증으로 넘어간다', () => {
    expect(validateLoginId('ab')).toBe('아이디는 4자 이상 입력해주세요.');
  });
});
