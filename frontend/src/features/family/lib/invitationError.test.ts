import { invitationErrorMessage } from './invitationError';

const res = (code: string, status = 400) => ({ response: { status, data: { code } } });

describe('invitationErrorMessage', () => {
  it('maps FAMILY_NOT_FOUND', () => {
    expect(invitationErrorMessage(res('FAMILY_NOT_FOUND', 404))).toContain('찾을 수 없');
  });

  it('maps FORBIDDEN', () => {
    expect(invitationErrorMessage(res('FORBIDDEN', 403))).toContain('부모만');
  });

  it('reports a network error', () => {
    expect(invitationErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('falls back for unknown codes', () => {
    expect(invitationErrorMessage(res('WHATEVER', 500))).toContain('잠시 후');
  });
});
