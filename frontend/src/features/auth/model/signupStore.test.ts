import { useSignupStore } from './signupStore';

beforeEach(() => useSignupStore.getState().reset());

describe('useSignupStore', () => {
  it('starts empty', () => {
    expect(useSignupStore.getState()).toMatchObject({
      email: '',
      emailVerified: false,
      role: null,
    });
  });

  it('patches values across steps', () => {
    useSignupStore.getState().patch({ email: 'a@b.com', emailVerified: true });
    useSignupStore.getState().patch({ loginId: 'parent01' });

    expect(useSignupStore.getState()).toMatchObject({
      email: 'a@b.com',
      emailVerified: true,
      loginId: 'parent01',
    });
  });

  it('reset clears everything', () => {
    useSignupStore.getState().patch({ email: 'a@b.com', role: 'PARENT', name: '영주' });
    useSignupStore.getState().reset();

    expect(useSignupStore.getState()).toMatchObject({ email: '', role: null, name: '' });
  });
});
