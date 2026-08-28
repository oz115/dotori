import { QueryClient } from '@tanstack/react-query';

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
