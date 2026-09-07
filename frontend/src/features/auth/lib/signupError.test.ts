import { signupErrorMessage } from './signupError';

describe('signupErrorMessage', () => {
  it('maps 409 to a duplicate message', () => {
    expect(signupErrorMessage({ response: { status: 409 } })).toContain('이미 사용 중');
  });

  it('maps 400 to a check-input message', () => {
    expect(signupErrorMessage({ response: { status: 400 } })).toContain('다시 확인');
  });

  it('falls back for other errors', () => {
    expect(signupErrorMessage({ response: { status: 500 } })).toContain('잠시 후');
    expect(signupErrorMessage(new Error('network'))).toContain('잠시 후');
  });
});
