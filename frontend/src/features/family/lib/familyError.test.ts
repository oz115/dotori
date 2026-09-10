import { createFamilyErrorMessage } from './familyError';

const res = (code: string, status = 400) => ({ response: { status, data: { code } } });

describe('createFamilyErrorMessage', () => {
  it('maps ALREADY_IN_FAMILY', () => {
    expect(createFamilyErrorMessage(res('ALREADY_IN_FAMILY', 409))).toContain('이미 가족');
  });

  it('maps FORBIDDEN', () => {
    expect(createFamilyErrorMessage(res('FORBIDDEN', 403))).toContain('부모 계정');
  });

  it('maps INVALID_REQUEST to an amount hint', () => {
    expect(createFamilyErrorMessage(res('INVALID_REQUEST'))).toContain('1,000원');
  });

  it('reports a network error', () => {
    expect(createFamilyErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(createFamilyErrorMessage(res('WHATEVER', 500))).toContain('잠시 후');
  });
});
