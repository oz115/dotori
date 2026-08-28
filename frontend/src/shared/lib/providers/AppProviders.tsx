import { QueryClientProvider } from '@tanstack/react-query';
import { DarkTheme, DefaultTheme, ThemeProvider } from 'expo-router';
import type { PropsWithChildren } from 'react';
import { useColorScheme } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { queryClient } from '@/shared/api';

/**
 * 앱 전체를 감싸는 전역 Provider 조합.
 * expo-router 의 app/_layout.tsx 는 이 컴포넌트 하나만 씌우면 되고,
 * 새 Provider(예: 인증 컨텍스트)가 추가될 때도 이 파일 안에서만 확장합니다.
 */
export function AppProviders({ children }: PropsWithChildren) {
  const colorScheme = useColorScheme();

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
            {children}
          </ThemeProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
