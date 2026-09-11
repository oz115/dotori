import { accountAuthRequestErrorMessage, accountVerifyErrorMessage } from './accountAuthError';

const res = (code: string, status = 400) => ({ response: { status, data: { code } } });

describe('accountAuthRequestErrorMessage', () => {
  it('maps ACCOUNT_ALREADY_LINKED', () => {
    expect(accountAuthRequestErrorMessage(res('ACCOUNT_ALREADY_LINKED', 409))).toContain(
      '이미 연동',
    );
  });

  it('maps FINANCE_API_ERROR to an account-number hint', () => {
    expect(accountAuthRequestErrorMessage(res('FINANCE_API_ERROR'))).toContain('계좌번호를 확인');
  });

  it('reports a network error', () => {
    expect(accountAuthRequestErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(accountAuthRequestErrorMessage(res('WHATEVER', 500))).toContain('잠시 후');
  });
});

describe('accountVerifyErrorMessage', () => {
  it('maps AUTH_CODE_MISMATCH', () => {
    expect(accountVerifyErrorMessage(res('AUTH_CODE_MISMATCH'))).toContain('일치하지 않');
  });

  it('maps ACCOUNT_ALREADY_LINKED', () => {
    expect(accountVerifyErrorMessage(res('ACCOUNT_ALREADY_LINKED', 409))).toContain('이미 연동');
  });

  it('reports a network error', () => {
    expect(accountVerifyErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(accountVerifyErrorMessage(res('WHATEVER', 500))).toContain('잠시 후');
  });
});
