import { cashoutErrorMessage, chargeErrorMessage, reviewCashoutErrorMessage } from './walletError';

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

describe('reviewCashoutErrorMessage', () => {
  it('maps known codes to Korean messages', () => {
    expect(reviewCashoutErrorMessage(apiError('CASHOUT_REQUEST_ALREADY_PROCESSED'))).toContain(
      '이미 처리된 요청',
    );
    expect(reviewCashoutErrorMessage(apiError('ACCOUNT_NOT_LINKED'))).toContain('연결된 계좌');
    expect(reviewCashoutErrorMessage(apiError('BANK_TRANSFER_FAILED'))).toContain('돌려줬어요');
  });

  it('separates an unknown outcome from a failure', () => {
    // 네트워크가 끊기면 송금이 됐는지 알 수 없다 - 실패라고 단정하면 안 된다
    expect(reviewCashoutErrorMessage({})).toContain('확인할 수 없어요');
    expect(reviewCashoutErrorMessage(apiError('SOMETHING_ELSE'))).toContain('처리에 실패');
  });
});
