import { deleteFamilyErrorMessage } from './deleteFamilyError';

const res = (code: string, status = 409) => ({ response: { status, data: { code } } });

describe('deleteFamilyErrorMessage', () => {
  it('아이에게 자산이 남아 있으면 무엇을 정리해야 하는지 알려준다', () => {
    const { title, message } = deleteFamilyErrorMessage(res('CHILD_HAS_ACTIVE_ASSETS'));

    expect(title).toBe('아직 삭제할 수 없어요');
    expect(message).toContain('도토리');
    expect(message).toContain('예적금');
    expect(message).toContain('투자');
    expect(message).toContain('환전 대기');
  });

  it('생성자가 아니면 그 이유를 알려준다', () => {
    expect(deleteFamilyErrorMessage(res('FORBIDDEN', 403)).message).toContain('만든 부모만');
  });

  it('네트워크 오류를 구분한다', () => {
    expect(deleteFamilyErrorMessage(new Error('network')).message).toContain('연결할 수 없');
  });

  it('알 수 없는 오류는 일반 문구로 안내한다', () => {
    expect(deleteFamilyErrorMessage(res('WHATEVER', 500)).message).toContain('잠시 후');
  });
});
