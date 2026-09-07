import { loginErrorMessage } from './loginError';

describe('loginErrorMessage', () => {
  it('maps 401/400 to a credentials message', () => {
    expect(loginErrorMessage({ response: { status: 401 } })).toContain('비밀번호');
    expect(loginErrorMessage({ response: { status: 400 } })).toContain('비밀번호');
  });

  it('falls back to a generic message otherwise', () => {
    expect(loginErrorMessage({ response: { status: 500 } })).toContain('잠시 후');
    expect(loginErrorMessage(new Error('network'))).toContain('잠시 후');
  });
});
