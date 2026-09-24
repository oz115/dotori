// Created: 2026-09-19 13:19:21
import { changePasswordErrorMessage, updateProfileErrorMessage } from './profileEditError';

const withCode = (code: string) => ({ response: { status: 400, data: { code } } });

describe('updateProfileErrorMessage', () => {
  it('maps INVALID_REQUEST', () => {
    expect(updateProfileErrorMessage(withCode('INVALID_REQUEST'))).toContain('이름');
  });

  it('reports a network error', () => {
    expect(updateProfileErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(updateProfileErrorMessage(withCode('SOMETHING_ELSE'))).toContain('잠시 후');
  });
});

describe('changePasswordErrorMessage', () => {
  it('maps PASSWORD_MISMATCH', () => {
    expect(changePasswordErrorMessage(withCode('PASSWORD_MISMATCH'))).toContain('현재 비밀번호');
  });

  it('maps INVALID_PASSWORD_FORMAT', () => {
    expect(changePasswordErrorMessage(withCode('INVALID_PASSWORD_FORMAT'))).toContain('8자');
  });

  it('reports a network error', () => {
    expect(changePasswordErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(changePasswordErrorMessage(withCode('SOMETHING_ELSE'))).toContain('잠시 후');
  });
});
