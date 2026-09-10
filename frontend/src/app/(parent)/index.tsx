import { router } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { signOut } from '@/features/auth';
import { Button } from '@/shared/ui';
import { useTheme } from '@/shared/theme';

/**
 * 부모 홈 자리 표시자. 실제 홈은 부모 홈 태스크에서 구현한다.
 *
 * 가족 유무에 따른 온보딩 분기는 `GET /families/me`(BE 예정)가 나오면
 * 라우트 가드로 옮긴다. 그전까지는 여기서 온보딩 진입점만 제공한다.
 */
export default function ParentHomeScreen() {
  const theme = useTheme();

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.color.background.canvas }]}>
      <View style={styles.center}>
        <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>부모 홈</Text>
        <Button
          label="우리 가족 만들기"
          variant="gradient"
          onPress={() => router.push('/onboarding')}
        />
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
