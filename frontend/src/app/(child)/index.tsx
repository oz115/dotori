import { Redirect } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { signOut } from '@/features/auth';
import { useFamilyStatus } from '@/features/family';
import { Button } from '@/shared/ui';
import { useTheme } from '@/shared/theme';

/**
 * 자녀 홈 자리 표시자. 실제 홈은 자녀 홈 태스크에서 구현한다.
 *
 * 가족에 연결되지 않은 자녀는 앱을 쓸 수 없으므로 셋업(가족 연동)으로 보낸다.
 * 상태 조회 전에는 아무것도 그리지 않아 깜빡임을 막는다.
 */
export default function ChildHomeScreen() {
  const theme = useTheme();
  const status = useFamilyStatus();

  // 첫 조회 중 — 깜빡임 방지
  if (status.isPending) {
    return <View style={{ flex: 1, backgroundColor: theme.color.background.canvas }} />;
  }

  // 가족이 확실히 있는 게 아니면(미연결 또는 조회 실패) 셋업으로.
  // 자녀는 가족 연결 없이는 앱을 쓸 수 없다.
  if (!status.data?.hasFamily) {
    return <Redirect href="/setup" />;
  }

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.color.background.canvas }]}>
      <View style={styles.center}>
        <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>자녀 홈</Text>
        <Button label="로그아웃" variant="secondary" onPress={signOut} />
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
