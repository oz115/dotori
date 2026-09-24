import { findIdErrorMessage, resetPasswordErrorMessage } from './accountRecoveryError';

const res = (code: string, status = 400) => ({ response: { status, data: { code } } });

describe('findIdErrorMessage', () => {
  it('maps USER_NOT_FOUND', () => {
    expect(findIdErrorMessage(res('USER_NOT_FOUND', 404))).toContain('찾을 수 없');
  });

  it('maps INVALID_VERIFICATION_TOKEN', () => {
    expect(findIdErrorMessage(res('INVALID_VERIFICATION_TOKEN', 401))).toContain('인증이 만료');
  });

  it('reports a network error', () => {
    expect(findIdErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(findIdErrorMessage(res('WHATEVER', 500))).toContain('잠시 후');
  });
});

describe('resetPasswordErrorMessage', () => {
  it('maps USER_NOT_FOUND', () => {
    expect(resetPasswordErrorMessage(res('USER_NOT_FOUND', 404))).toContain('찾을 수 없');
  });

  it('maps INVALID_PASSWORD_FORMAT', () => {
    expect(resetPasswordErrorMessage(res('INVALID_PASSWORD_FORMAT'))).toContain('8자 이상');
  });

  it('maps INVALID_VERIFICATION_TOKEN', () => {
    expect(resetPasswordErrorMessage(res('INVALID_VERIFICATION_TOKEN', 401))).toContain(
      '인증이 만료',
    );
  });

  it('reports a network error', () => {
    expect(resetPasswordErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(resetPasswordErrorMessage(res('WHATEVER', 500))).toContain('잠시 후');
  });
});
