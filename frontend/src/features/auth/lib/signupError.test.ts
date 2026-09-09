import { signupErrorMessage } from './signupError';

const withCode = (code: string) => ({ response: { status: 400, data: { code } } });

describe('signupErrorMessage', () => {
  it('maps DUPLICATE_LOGIN_ID', () => {
    expect(signupErrorMessage(withCode('DUPLICATE_LOGIN_ID'))).toContain('아이디');
  });

  it('maps DUPLICATE_EMAIL', () => {
    expect(signupErrorMessage(withCode('DUPLICATE_EMAIL'))).toContain('이메일');
  });

  it('maps INVALID_VERIFICATION_TOKEN', () => {
    expect(signupErrorMessage(withCode('INVALID_VERIFICATION_TOKEN'))).toContain('인증');
  });

  it('reports a network error', () => {
    expect(signupErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(signupErrorMessage(withCode('SOMETHING_ELSE'))).toContain('잠시 후');
  });
});
