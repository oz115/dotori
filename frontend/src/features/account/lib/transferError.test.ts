import { transferErrorMessage } from './transferError';

const res = (code: string, status = 400) => ({ response: { status, data: { code } } });

describe('transferErrorMessage', () => {
  it('maps ACCOUNT_NOT_LINKED', () => {
    expect(transferErrorMessage(res('ACCOUNT_NOT_LINKED', 404))).toContain('연동된 계좌가 없');
  });

  it('maps SELF_TRANSFER_NOT_ALLOWED', () => {
    expect(transferErrorMessage(res('SELF_TRANSFER_NOT_ALLOWED'))).toContain('내 계좌로는');
  });

  it('maps TRANSFER_LIMIT_EXCEEDED', () => {
    expect(transferErrorMessage(res('TRANSFER_LIMIT_EXCEEDED'))).toContain('한도를 넘었');
  });

  it('maps INSUFFICIENT_ACCOUNT_BALANCE', () => {
    expect(transferErrorMessage(res('INSUFFICIENT_ACCOUNT_BALANCE'))).toContain('잔액이 부족');
  });

  it('maps BANK_TRANSFER_FAILED to an account-number hint', () => {
    expect(transferErrorMessage(res('BANK_TRANSFER_FAILED', 502))).toContain('계좌번호를 확인');
  });

  it('reports a network error', () => {
    expect(transferErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(transferErrorMessage(res('WHATEVER', 500))).toContain('잠시 후');
  });
});
