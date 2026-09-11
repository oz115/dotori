import { Redirect, router } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useMyAccount } from '@/features/account';
import { signOut, WithdrawButton } from '@/features/auth';
import { useFamilyStatus } from '@/features/family';
import { Button } from '@/shared/ui';
import { useTheme } from '@/shared/theme';

/**
 * 자녀 홈 자리 표시자. 실제 홈은 자녀 홈 태스크에서 구현한다.
 *
 * 자녀는 (1) 가족 연결 (2) 계좌 연동을 모두 끝내야 앱을 쓸 수 있으므로,
 * 둘 중 하나라도 안 됐으면 셋업 화면으로 보낸다. 조회 전에는 아무것도 그리지 않아 깜빡임을 막는다.
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

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.color.background.canvas }]}>
      <View style={styles.center}>
        <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>자녀 홈</Text>
        {/* 임시: 홈이 자리 표시자인 동안 투자 탭으로 들어갈 통로. 자녀 홈 태스크에서 제거한다 */}
        <Button label="투자 화면 (임시)" variant="primary" onPress={() => router.push('/invest')} />
        <Button label="로그아웃" variant="secondary" onPress={signOut} />
        <WithdrawButton />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
    paddingHorizontal: 24,
  },
});
