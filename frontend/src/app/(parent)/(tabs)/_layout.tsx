import { Redirect, Tabs } from 'expo-router';
import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useFamilyReady } from '@/features/family';
import { useTheme } from '@/shared/theme';
import { TabScene, tabIcon, tabScreenOptions } from '@/shared/ui';

/**
 * 탭은 화면 순서(할 일·상점·홈·저축·투자)대로 선언하는데, 그러면 첫 번째인 '할 일'이
 * 진입 탭이 된다. 앱을 켜면 홈이 나와야 하므로 시작 탭을 따로 지정한다.
 */
export const unstable_settings = { initialRouteName: 'index' };

/**
 * 부모 탭 레이아웃. 가족을 만들고 아이가 합류한 부모만 탭 화면들을 볼 수 있다.
 * 그 전에는 하단 탭이 없는 온보딩 허브로 보낸다 — 자녀 탭 레이아웃이 (child)/setup 으로
 * 보내는 것과 같은 방식이다.
 */
export default function ParentTabsLayout() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const family = useFamilyReady();

  if (family.isPending) {
    return <View style={{ flex: 1, backgroundColor: theme.color.background.canvas }} />;
  }

  if (!family.ready) {
    return <Redirect href="/onboarding" />;
  }

  return (
    <Tabs
      initialRouteName="index"
      backBehavior="initialRoute"
      screenOptions={tabScreenOptions(theme, insets.bottom)}
      screenLayout={({ children }) => <TabScene>{children}</TabScene>}
    >
      <Tabs.Screen name="task" options={{ tabBarLabel: '미션', tabBarIcon: tabIcon('task') }} />
      <Tabs.Screen name="store" options={{ tabBarLabel: '상점', tabBarIcon: tabIcon('store') }} />
      <Tabs.Screen name="index" options={{ tabBarLabel: '홈', tabBarIcon: tabIcon('home') }} />
      <Tabs.Screen name="save" options={{ tabBarLabel: '저축', tabBarIcon: tabIcon('save') }} />
      <Tabs.Screen
        name="investment"
        options={{ tabBarLabel: '투자', tabBarIcon: tabIcon('invest') }}
      />
    </Tabs>
  );
}
