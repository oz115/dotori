import { Redirect } from 'expo-router';
import { View } from 'react-native';

import { useMyAccount } from '@/features/account';
import { useFamilyStatus } from '@/features/family';
import { useTheme } from '@/shared/theme';
import { ChildHome } from '@/widgets/child-home';
import { MainTabBar } from '@/widgets/main-tab-bar';

/**
 * 자녀는 (1) 가족 연결 (2) 계좌 연동을 모두 끝내야 앱을 쓸 수 있으므로,
 * 둘 중 하나라도 안 됐으면 셋업 화면으로 보낸다. 조회 전에는 아무것도 그리지 않아 깜빡임을 막는다.
 * 라우트 파일은 이 가드와 widget 조립(화면 + 하단 탭바)만 맡는다.
 * widget끼리는 서로 import 하지 않으므로 여기서 묶어 넘겨준다.
 */
export default function ChildHomeScreen() {
  const theme = useTheme();
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

  return <ChildHome tabBar={<MainTabBar active="home" />} />;
}
