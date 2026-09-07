// react-native-safe-area-context 는 Provider 없이 useSafeAreaInsets 를 호출하면 경고를 낸다.
// 공식 목으로 교체해 컴포넌트 테스트에서 인셋 0을 반환하게 한다.
import mockSafeAreaContext from 'react-native-safe-area-context/jest/mock';

jest.mock('react-native-safe-area-context', () => mockSafeAreaContext);

// expo-secure-store 는 네이티브 모듈이라 테스트 환경에서 인메모리로 대체한다.
jest.mock('expo-secure-store', () => {
  const store = new Map();
  return {
    WHEN_UNLOCKED_THIS_DEVICE_ONLY: 'WHEN_UNLOCKED_THIS_DEVICE_ONLY',
    getItemAsync: jest.fn((key) => Promise.resolve(store.has(key) ? store.get(key) : null)),
    setItemAsync: jest.fn((key, value) => {
      store.set(key, value);
      return Promise.resolve();
    }),
    deleteItemAsync: jest.fn((key) => {
      store.delete(key);
      return Promise.resolve();
    }),
  };
});
