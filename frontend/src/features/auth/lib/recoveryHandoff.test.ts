import { recoveryHandoff } from './recoveryHandoff';

const verified = { email: 'a@b.com', verificationToken: 'vt_123' };

afterEach(() => recoveryHandoff.clear());

describe('recoveryHandoff', () => {
  it('넣어 둔 인증 결과를 읽을 수 있고, 읽어도 비워지지 않는다', () => {
    recoveryHandoff.put(verified);
    expect(recoveryHandoff.peek()).toEqual(verified);
    expect(recoveryHandoff.peek()).toEqual(verified);
  });

  it('clear 하면 null 이다', () => {
    recoveryHandoff.put(verified);
    recoveryHandoff.clear();
    expect(recoveryHandoff.peek()).toBeNull();
  });

  it('아무것도 넣지 않았으면 null', () => {
    expect(recoveryHandoff.peek()).toBeNull();
  });

  it('유효시간(9분)이 지나면 버린다', () => {
    recoveryHandoff.put(verified, 1_000);
    expect(recoveryHandoff.peek(1_000 + 9 * 60 * 1000 + 1)).toBeNull();
  });
});
