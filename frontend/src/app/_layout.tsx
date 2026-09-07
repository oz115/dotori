import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';

import { AppProviders } from '@/shared/lib/providers';
import { useAppFonts } from '@/shared/theme';

SplashScreen.preventAutoHideAsync();

/**
 * expo-router 진입점. 라우팅과 전역 Provider 조립만 담당하고,
 * 실제 화면 UI는 전부 src/pages/* 로 위임합니다.
 */
export default function RootLayout() {
  const [fontsLoaded, fontError] = useAppFonts();

  useEffect(() => {
    if (fontsLoaded || fontError) {
      SplashScreen.hideAsync();
    }
  }, [fontsLoaded, fontError]);

  if (!fontsLoaded && !fontError) {
    return null;
  }

  return (
    <AppProviders>
      <Tabs screenOptions={{ headerShown: false }}>
        <Tabs.Screen
          name="index"
          options={{
            title: 'Home',
            tabBarIcon: ({ color, size }) => <Ionicons name="home" color={color} size={size} />,
          }}
        />
        <Tabs.Screen
          name="explore"
          options={{
            title: 'Explore',
            tabBarIcon: ({ color, size }) => <Ionicons name="compass" color={color} size={size} />,
          }}
        />
      </Tabs>
    </AppProviders>
  );
}
