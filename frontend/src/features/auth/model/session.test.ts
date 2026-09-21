import { authApi } from '@/features/auth/api/authApi';
import { savedLoginId } from '@/features/auth/lib/savedLoginId';
import { tokenStorage } from '@/features/auth/lib/tokenStorage';
import { useAuthStore } from '@/features/auth/model/authStore';
import { restoreSession, signIn, signOut, withdrawAccount } from '@/features/auth/model/session';
import { queryClient } from '@/shared/api';
import { clearSessionScopedStorage } from '@/shared/lib/sessionScopedStorage';

jest.mock('@/features/auth/api/authApi');
jest.mock('@/features/auth/lib/tokenStorage');
jest.mock('@/features/auth/lib/savedLoginId');
jest.mock('@/shared/lib/sessionScopedStorage');

const mockedApi = jest.mocked(authApi);
const mockedStorage = jest.mocked(tokenStorage);
const mockedSessionStorage = jest.mocked(clearSessionScopedStorage);
const mockedSavedLoginId = jest.mocked(savedLoginId);

// payload: {"sub":"7","role":"PARENT","exp":9999999999}
const PARENT_ACCESS = 'x.eyJzdWIiOiI3Iiwicm9sZSI6IlBBUkVOVCIsImV4cCI6OTk5OTk5OTk5OX0.y';

beforeEach(() => {
  jest.clearAllMocks();
  mockedSavedLoginId.clear.mockResolvedValue(undefined);
  useAuthStore.setState({ status: 'loading', accessToken: null, user: null });
});

describe('signIn', () => {
  it('stores the refresh token and sets an authenticated session', async () => {
    mockedApi.login.mockResolvedValue({
      accessToken: 'access-1',
      tokenType: 'Bearer',
      expiresIn: 900,
      refreshToken: 'refresh-1',
      user: { userId: 7, name: '영주', role: 'PARENT' },
    });

    await signIn({ loginId: 'yj', password: 'pw12345678' });

    expect(mockedStorage.set).toHaveBeenCalledWith('refresh-1');
    expect(useAuthStore.getState()).toMatchObject({
      status: 'authenticated',
      accessToken: 'access-1',
      user: { userId: 7, role: 'PARENT', name: '영주' },
    });
  });
});

describe('withdrawAccount', () => {
  it('탈퇴하면 세션과 함께 "아이디 저장" 값도 지운다', async () => {
    useAuthStore.setState({
      status: 'authenticated',
      accessToken: 'a',
      user: { userId: 7, role: 'PARENT' },
    });
    mockedApi.withdraw.mockResolvedValue(undefined as never);

    await withdrawAccount('pw12345678');

    expect(mockedApi.withdraw).toHaveBeenCalledWith(7, 'pw12345678');
    expect(mockedSavedLoginId.clear).toHaveBeenCalled();
    expect(useAuthStore.getState().status).toBe('guest');
  });

  it('비밀번호가 틀려 실패하면 세션도 저장된 아이디도 그대로 둔다', async () => {
    useAuthStore.setState({
      status: 'authenticated',
      accessToken: 'a',
      user: { userId: 7, role: 'PARENT' },
    });
    mockedApi.withdraw.mockRejectedValue(new Error('PASSWORD_MISMATCH'));

    await expect(withdrawAccount('wrong')).rejects.toThrow('PASSWORD_MISMATCH');

    expect(mockedSavedLoginId.clear).not.toHaveBeenCalled();
    expect(useAuthStore.getState().status).toBe('authenticated');
  });
});

describe('signOut', () => {
  it('clears storage and session even if the server call fails', async () => {
    useAuthStore.setState({
      status: 'authenticated',
      accessToken: 'a',
      user: { userId: 7, role: 'PARENT' },
    });
    mockedApi.logout.mockRejectedValue(new Error('network'));

    await signOut();

    expect(mockedStorage.clear).toHaveBeenCalled();
    expect(useAuthStore.getState()).toMatchObject({
      status: 'guest',
      accessToken: null,
      user: null,
    });
  });

  it('wipes the query cache and session-scoped storage so the next user sees nothing', async () => {
    useAuthStore.setState({
      status: 'authenticated',
      accessToken: 'a',
      user: { userId: 7, role: 'PARENT' },
    });
    mockedApi.logout.mockResolvedValue(undefined as never);
    // 이전 사용자의 계좌 정보가 캐시에 남아 있는 상황
    queryClient.setQueryData(['account', 'me'], { accountBalance: 13250 });

    await signOut();

    expect(queryClient.getQueryData(['account', 'me'])).toBeUndefined();
    expect(mockedSessionStorage).toHaveBeenCalled();
  });
});

describe('restoreSession', () => {
  it('refreshes and authenticates when a refresh token exists', async () => {
    mockedStorage.get.mockResolvedValue('refresh-old');
    mockedApi.refresh.mockResolvedValue({
      accessToken: PARENT_ACCESS,
      tokenType: 'Bearer',
      expiresIn: 900,
      refreshToken: 'refresh-new',
    });

    await restoreSession();

    expect(mockedStorage.set).toHaveBeenCalledWith('refresh-new');
    expect(useAuthStore.getState()).toMatchObject({
      status: 'authenticated',
      user: { userId: 7, role: 'PARENT' },
    });
  });

  it('falls back to guest when there is no refresh token', async () => {
    mockedStorage.get.mockResolvedValue(null);

    await restoreSession();

    expect(useAuthStore.getState().status).toBe('guest');
  });

  it('falls back to guest when refresh fails', async () => {
    mockedStorage.get.mockResolvedValue('refresh-expired');
    mockedApi.refresh.mockRejectedValue(new Error('401'));

    await restoreSession();

    expect(mockedStorage.clear).toHaveBeenCalled();
    expect(useAuthStore.getState().status).toBe('guest');
  });
});
