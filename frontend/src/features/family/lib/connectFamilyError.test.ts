import { connectFamilyErrorMessage } from './connectFamilyError';

const res = (code: string, status = 400) => ({ response: { status, data: { code } } });

describe('connectFamilyErrorMessage', () => {
  it('maps INVALID_FAMILY_CODE', () => {
    expect(connectFamilyErrorMessage(res('INVALID_FAMILY_CODE'))).toContain('코드가 올바르지 않');
  });

  it('maps ALREADY_IN_FAMILY', () => {
    expect(connectFamilyErrorMessage(res('ALREADY_IN_FAMILY', 409))).toContain('이미 가족');
  });

  it('maps FORBIDDEN', () => {
    expect(connectFamilyErrorMessage(res('FORBIDDEN', 403))).toContain('자녀 계정만');
  });

  it('reports a network error', () => {
    expect(connectFamilyErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(connectFamilyErrorMessage(res('WHATEVER', 500))).toContain('잠시 후');
  });
});
