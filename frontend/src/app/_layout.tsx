import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';

import { restoreSession, setupAuthInterceptor, useAuthStore } from '@/features/auth';
import { ensurePushRegistered, useNotificationNavigation } from '@/features/notification';
import { AppProviders } from '@/shared/lib/providers';
import { useAppFonts } from '@/shared/theme';

SplashScreen.preventAutoHideAsync();
setupAuthInterceptor();

/**
 * expo-router 진입점. 폰트 로딩과 세션 복원이 끝날 때까지 스플래시를 유지하고,
 * 인증 상태에 따라 라우트 그룹((auth)/(parent)/(child)) 중 하나만 노출한다.
 * 역할별 탭 화면(홈·투자 등)은 각 그룹 안의 (tabs) 레이아웃이 갖고 있다.
 * 화면 UI는 각 그룹으로 위임한다.
 */
export default function RootLayout() {
  const [fontsLoaded, fontError] = useAppFonts();
  const status = useAuthStore((state) => state.status);
  const role = useAuthStore((state) => state.user?.role);

  useEffect(() => {
    restoreSession();
  }, []);

  // 수동 로그인이든 부팅 시 세션 복원이든, authenticated 로 바뀔 때마다 한 번씩
  // 조용히 등록을 시도한다 (거부했던 사용자에게 매번 다시 묻지 않음, 실패해도 무시).
  useEffect(() => {
    if (status === 'authenticated') {
      ensurePushRegistered(false);
    }
  }, [status]);

  // 알림을 탭해서 앱을 열었을 때(콜드 스타트 포함) 그 알림이 가리키는 화면으로 이동한다.
  useNotificationNavigation(role);

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
