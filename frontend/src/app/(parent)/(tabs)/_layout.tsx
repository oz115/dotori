import { Tabs } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useTheme } from '@/shared/theme';
import { tabIcon, tabScreenOptions } from '@/shared/ui';

/**
 * 탭은 화면 순서(할 일·상점·홈·저축·투자)대로 선언하는데, 그러면 첫 번째인 '할 일'이
 * 진입 탭이 된다. 앱을 켜면 홈이 나와야 하므로 시작 탭을 따로 지정한다.
 */
export const unstable_settings = { initialRouteName: 'index' };

/** 부모 탭 레이아웃. 부모는 자녀와 달리 탭 진입에 별도 가드가 없다. */
export default function ParentTabsLayout() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <Tabs
      initialRouteName="index"
      backBehavior="initialRoute"
      screenOptions={tabScreenOptions(theme, insets.bottom)}
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
