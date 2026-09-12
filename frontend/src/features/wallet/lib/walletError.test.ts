import { cashoutErrorMessage, chargeErrorMessage } from './walletError';

function apiError(code: string) {
  return { response: { data: { code } } };
}

describe('chargeErrorMessage', () => {
  it('maps known codes to Korean messages', () => {
    expect(chargeErrorMessage(apiError('INVALID_CHARGE_UNIT'))).toContain('10원 단위');
    expect(chargeErrorMessage(apiError('INSUFFICIENT_ACCOUNT_BALANCE'))).toContain('잔액이 부족');
    expect(chargeErrorMessage(apiError('BANK_TRANSFER_FAILED'))).toContain('송금');
  });

  it('falls back for unknown codes and network errors', () => {
    expect(chargeErrorMessage(apiError('SOMETHING_ELSE'))).toContain('충전에 실패');
    expect(chargeErrorMessage({})).toContain('서버에 연결할 수 없어요');
  });
});

describe('cashoutErrorMessage', () => {
  it('maps known codes to Korean messages', () => {
    expect(cashoutErrorMessage(apiError('INSUFFICIENT_POINT'))).toContain('포인트가 부족');
    expect(cashoutErrorMessage(apiError('ACCOUNT_NOT_LINKED'))).toContain('연동된 계좌');
  });

  it('falls back for unknown codes and network errors', () => {
    expect(cashoutErrorMessage(apiError('SOMETHING_ELSE'))).toContain('환전 요청에 실패');
    expect(cashoutErrorMessage({})).toContain('서버에 연결할 수 없어요');
  });
});
