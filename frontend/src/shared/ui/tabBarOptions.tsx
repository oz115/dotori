import type { ColorValue } from 'react-native';

import { fontFamily, type Theme } from '@/shared/theme';
import { navIcons, type NavIconName } from '@/shared/ui/icons';

/**
 * 역할별 `(tabs)/_layout.tsx`가 공유하는 하단 탭바 스타일. 부모·자녀 두 곳에서
 * 똑같이 쓰이는데 여기가 갈라지면 두 탭바가 조금씩 달라 보이게 되니 한 곳에 둔다.
 *
 * `<Tabs screenOptions={...}>` 가 받는 타입을 그대로 쓰지 않고(현재 expo-router 버전에서
 * 공개 API로 안 나와 있음) 구조만 맞춰 리턴한다 — `<Tabs>` 쪽에서 대입할 때 검사된다.
 */
export function tabScreenOptions(theme: Theme, bottomInset: number) {
  return {
    headerShown: false,
    tabBarShowLabel: true,
    tabBarActiveTintColor: theme.color.brand.black,
    tabBarInactiveTintColor: theme.color.text.muted,
    tabBarLabelStyle: { fontFamily: fontFamily.regular, fontSize: 11 },
    tabBarStyle: {
      backgroundColor: theme.color.background.surface,
      borderTopLeftRadius: theme.radius['2xl'],
      borderTopRightRadius: theme.radius['2xl'],
      borderTopWidth: 0,
      paddingTop: 12,
      height: 58 + bottomInset,
    },
  };
}

/** `Tabs.Screen options={{ tabBarIcon: tabIcon('home') }}` 형태로 쓴다. */
export function tabIcon(name: NavIconName) {
  const Icon = navIcons[name];

  function TabBarIcon({ color }: { color: ColorValue }) {
    return <Icon size={26} color={String(color)} />;
  }

  return TabBarIcon;
}
