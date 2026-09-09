import { apiErrorCode, isNetworkError } from './apiError';

describe('apiErrorCode', () => {
  it('reads response.data.code', () => {
    expect(apiErrorCode({ response: { data: { code: 'DUPLICATE_LOGIN_ID' } } })).toBe(
      'DUPLICATE_LOGIN_ID',
    );
  });

  it('is undefined without a code', () => {
    expect(apiErrorCode({ response: { data: {} } })).toBeUndefined();
    expect(apiErrorCode(new Error('x'))).toBeUndefined();
  });
});

describe('isNetworkError', () => {
  it('true when there is no response', () => {
    expect(isNetworkError({ message: 'Network Error' })).toBe(true);
  });

  it('false when a response exists', () => {
    expect(isNetworkError({ response: { status: 500 } })).toBe(false);
  });
});
