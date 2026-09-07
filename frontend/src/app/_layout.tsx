import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';

import { restoreSession, setupAuthInterceptor, useAuthStore } from '@/features/auth';
import { AppProviders } from '@/shared/lib/providers';
import { useAppFonts } from '@/shared/theme';

SplashScreen.preventAutoHideAsync();
setupAuthInterceptor();

/**
 * expo-router 진입점. 폰트 로딩과 세션 복원이 끝날 때까지 스플래시를 유지하고,
 * 인증 상태에 따라 라우트 그룹((auth)/(parent)/(child)) 중 하나만 노출한다.
 * 화면 UI는 각 그룹으로 위임한다.
 */
export default function RootLayout() {
  const [fontsLoaded, fontError] = useAppFonts();
  const status = useAuthStore((state) => state.status);
  const role = useAuthStore((state) => state.user?.role);

  useEffect(() => {
    restoreSession();
  }, []);

  const ready = (fontsLoaded || fontError) && status !== 'loading';

  useEffect(() => {
    if (ready) {
      SplashScreen.hideAsync();
    }
  }, [ready]);

  if (!ready) {
    return null;
  }

  return (
    <AppProviders>
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Protected guard={status === 'guest'}>
          <Stack.Screen name="(auth)" />
        </Stack.Protected>
        <Stack.Protected guard={status === 'authenticated' && role === 'PARENT'}>
          <Stack.Screen name="(parent)" />
        </Stack.Protected>
        <Stack.Protected guard={status === 'authenticated' && role === 'CHILD'}>
          <Stack.Screen name="(child)" />
        </Stack.Protected>
      </Stack>
    </AppProviders>
  );
}
