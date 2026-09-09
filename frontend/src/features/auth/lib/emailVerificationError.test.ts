import { emailVerificationErrorMessage } from './emailVerificationError';

describe('emailVerificationErrorMessage', () => {
  it('maps INVALID_CODE', () => {
    expect(
      emailVerificationErrorMessage({ response: { status: 400, data: { code: 'INVALID_CODE' } } }),
    ).toContain('일치하지 않');
  });

  it('maps DUPLICATE_EMAIL', () => {
    expect(
      emailVerificationErrorMessage({
        response: { status: 409, data: { code: 'DUPLICATE_EMAIL' } },
      }),
    ).toContain('이미 가입');
  });

  it('reports a network error', () => {
    expect(emailVerificationErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(emailVerificationErrorMessage({ response: { status: 500, data: {} } })).toContain(
      '잠시 후',
    );
  });
});
