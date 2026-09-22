import { removeChildErrorMessage } from './removeChildError';

const res = (code: string, status = 409) => ({ response: { status, data: { code } } });

describe('removeChildErrorMessage', () => {
  it('자산이 남아 있으면 무엇을 정리해야 하는지 알려준다', () => {
    // 서버가 막는 이유를 그대로 옮긴다 - 내보내고 나면 그 자산을 정리할 길이 끊긴다.
    const { title, message } = removeChildErrorMessage(res('CHILD_HAS_ACTIVE_ASSETS'));

    expect(title).toBe('아직 제외할 수 없어요');
    expect(message).toContain('도토리');
    expect(message).toContain('예적금');
    expect(message).toContain('투자');
    expect(message).toContain('환전 대기');
  });

  it('이미 나간 아이면 목록을 새로고침하라고 한다', () => {
    expect(removeChildErrorMessage(res('MEMBER_NOT_FOUND', 404)).message).toContain('이미 나간');
  });

  it('부모가 아니면 그 이유를 알려준다', () => {
    expect(removeChildErrorMessage(res('FORBIDDEN', 403)).message).toContain('부모만');
  });

  it('네트워크 오류를 구분한다', () => {
    expect(removeChildErrorMessage(new Error('network')).message).toContain('연결할 수 없');
  });

  it('알 수 없는 오류는 일반 문구로 안내한다', () => {
    expect(removeChildErrorMessage(res('WHATEVER', 500)).message).toContain('잠시 후');
  });
});
