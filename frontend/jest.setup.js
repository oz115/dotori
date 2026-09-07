// react-native-safe-area-context 는 Provider 없이 useSafeAreaInsets 를 호출하면 경고를 낸다.
// 공식 목으로 교체해 컴포넌트 테스트에서 인셋 0을 반환하게 한다.
import mockSafeAreaContext from 'react-native-safe-area-context/jest/mock';

jest.mock('react-native-safe-area-context', () => mockSafeAreaContext);
