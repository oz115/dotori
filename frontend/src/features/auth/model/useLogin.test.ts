import { savedLoginId } from '@/features/auth/lib/savedLoginId';
import * as session from '@/features/auth/model/session';
import { performLogin } from '@/features/auth/model/useLogin';

jest.mock('@/features/auth/model/session');
jest.mock('@/features/auth/lib/savedLoginId');

const mockedSignIn = jest.mocked(session.signIn);
const mockedSaved = jest.mocked(savedLoginId);

beforeEach(() => {
  jest.clearAllMocks();
  mockedSignIn.mockResolvedValue(undefined);
});

describe('performLogin', () => {
  it('signs in and saves the id when rememberId is true', async () => {
    await performLogin({
      loginId: 'parent01',
      password: 'pw12345678',
      rememberId: true,
    });

    expect(mockedSignIn).toHaveBeenCalledWith({ loginId: 'parent01', password: 'pw12345678' });
    expect(mockedSaved.set).toHaveBeenCalledWith('parent01');
    expect(mockedSaved.clear).not.toHaveBeenCalled();
  });

  it('clears the saved id when rememberId is false', async () => {
    await performLogin({
      loginId: 'parent01',
      password: 'pw12345678',
      rememberId: false,
    });

    expect(mockedSaved.clear).toHaveBeenCalled();
    expect(mockedSaved.set).not.toHaveBeenCalled();
  });

  it('아이디 저장이 실패해도 로그인은 성공으로 끝난다', async () => {
    mockedSaved.set.mockRejectedValue(new Error('secure store unavailable'));

    await expect(
      performLogin({
        loginId: 'parent01',
        password: 'pw12345678',
        rememberId: true,
      }),
    ).resolves.toBeUndefined();
  });

  it('propagates the error when sign-in fails', async () => {
    mockedSignIn.mockRejectedValue(new Error('401'));

    await expect(performLogin({ loginId: 'x', password: 'y', rememberId: false })).rejects.toThrow(
      '401',
    );
  });
});
