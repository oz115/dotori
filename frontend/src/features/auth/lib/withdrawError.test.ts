import { withdrawErrorMessage } from './withdrawError';

const res = (code: string, status = 409) => ({ response: { status, data: { code } } });

describe('withdrawErrorMessage', () => {
  it('비밀번호가 틀리면 그 이유를 알려준다', () => {
    expect(withdrawErrorMessage(res('PASSWORD_MISMATCH', 401))).toContain('비밀번호가 일치하지');
  });

  it('부모: 가족에 아이가 남아 있으면 가족 관리로 안내한다', () => {
    const message = withdrawErrorMessage(res('FAMILY_HAS_CHILDREN'));

    expect(message).toContain('아이가 남아');
    expect(message).toContain('가족 관리');
  });

  it('자녀: 자산이 남아 있으면 정리할 것들을 알려준다', () => {
    const message = withdrawErrorMessage(res('HAS_ACTIVE_ASSETS'));

    expect(message).toContain('도토리');
    expect(message).toContain('예적금');
    expect(message).toContain('투자');
    expect(message).toContain('환전 대기');
  });

  it('네트워크 오류를 구분한다', () => {
    expect(withdrawErrorMessage(new Error('network'))).toContain('연결할 수 없');
  });

  it('알 수 없는 오류는 일반 문구로 안내한다', () => {
    expect(withdrawErrorMessage(res('WHATEVER', 500))).toContain('잠시 후');
  });
});
