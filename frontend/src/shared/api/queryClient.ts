import { focusManager, QueryClient } from '@tanstack/react-query';
import { AppState, type AppStateStatus } from 'react-native';

/**
 * 앱 전역에서 하나만 존재해야 하는 TanStack Query 클라이언트.
 * shared/providers/AppProviders 에서 QueryClientProvider 로 주입됩니다.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000,
      retry: 1,
    },
  },
});

/**
 * 앱이 다시 화면에 나타나면 오래된 데이터를 새로 받아온다.
 *
 * TanStack Query 의 "창 포커스" 감지는 웹 기준이라 RN 에서는 동작하지 않는다.
 * 그래서 AppState 를 대신 연결한다 — 부모가 앱을 잠깐 내렸다 다시 열었을 때
 * 아이가 보낸 승인 요청이 그대로 낡은 채로 남아 있지 않게 하려는 것.
 */
AppState.addEventListener('change', (status: AppStateStatus) => {
  focusManager.setFocused(status === 'active');
});
