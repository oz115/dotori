import { Tabs } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useTheme } from '@/shared/theme';
import { tabIcon, tabScreenOptions } from '@/shared/ui';

/** 부모 탭 레이아웃. 부모는 자녀와 달리 탭 진입에 별도 가드가 없다. */
export default function ParentTabsLayout() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <Tabs screenOptions={tabScreenOptions(theme, insets.bottom)}>
      <Tabs.Screen name="task" options={{ tabBarLabel: '할 일', tabBarIcon: tabIcon('task') }} />
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
