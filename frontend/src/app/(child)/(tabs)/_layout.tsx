import { Redirect, Tabs } from 'expo-router';
import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useMyAccount } from '@/features/account';
import { useFamilyStatus } from '@/features/family';
import { useTheme } from '@/shared/theme';
import { tabIcon, tabScreenOptions } from '@/shared/ui';

/**
 * 탭은 화면 순서(할 일·상점·홈·저축·투자)대로 선언하는데, 그러면 첫 번째인 '할 일'이
 * 진입 탭이 된다. 앱을 켜면 홈이 나와야 하므로 시작 탭을 따로 지정한다.
 */
export const unstable_settings = { initialRouteName: 'index' };

/**
 * 자녀 탭 레이아웃. 가족 연결·계좌 연동이 끝난 자녀만 탭 화면들을 볼 수 있어서
 * (child)/index.tsx 에 있던 가드를 여기로 옮겼다 — 탭 밖(setup)으로 보내는 판단은
 * 탭이 생기기 전에 해야 하므로 레이아웃이 맡는 게 맞다.
 */
export default function ChildTabsLayout() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const family = useFamilyStatus();
  const account = useMyAccount();

  if (family.isPending || account.isPending) {
    return <View style={{ flex: 1, backgroundColor: theme.color.background.canvas }} />;
  }

  const hasFamily = family.data?.hasFamily === true;
  const hasAccount = account.data != null; // ACCOUNT_NOT_LINKED 면 data 없음

  if (!hasFamily || !hasAccount) {
    return <Redirect href="/setup" />;
  }

  return (
    <Tabs
      initialRouteName="index"
      backBehavior="initialRoute"
      screenOptions={tabScreenOptions(theme, insets.bottom)}
    >
      <Tabs.Screen name="task" options={{ tabBarLabel: '할 일', tabBarIcon: tabIcon('task') }} />
      <Tabs.Screen name="store" options={{ tabBarLabel: '상점', tabBarIcon: tabIcon('store') }} />
      <Tabs.Screen name="index" options={{ tabBarLabel: '홈', tabBarIcon: tabIcon('home') }} />
      <Tabs.Screen name="save" options={{ tabBarLabel: '저축', tabBarIcon: tabIcon('save') }} />
      <Tabs.Screen name="invest" options={{ tabBarLabel: '투자', tabBarIcon: tabIcon('invest') }} />
    </Tabs>
  );
}
