import { authApi } from '@/features/auth/api/authApi';
import { tokenStorage } from '@/features/auth/lib/tokenStorage';
import { useAuthStore } from '@/features/auth/model/authStore';
import { restoreSession, signIn, signOut } from '@/features/auth/model/session';

jest.mock('@/features/auth/api/authApi');
jest.mock('@/features/auth/lib/tokenStorage');

const mockedApi = jest.mocked(authApi);
const mockedStorage = jest.mocked(tokenStorage);

// payload: {"sub":"7","role":"PARENT","exp":9999999999}
const PARENT_ACCESS = 'x.eyJzdWIiOiI3Iiwicm9sZSI6IlBBUkVOVCIsImV4cCI6OTk5OTk5OTk5OX0.y';

beforeEach(() => {
  jest.clearAllMocks();
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
