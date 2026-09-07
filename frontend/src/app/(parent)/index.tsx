import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { signOut } from '@/features/auth';
import { Button } from '@/shared/ui';
import { useTheme } from '@/shared/theme';

/**
 * 부모 홈 자리 표시자. 실제 홈은 부모 홈 태스크에서 구현한다.
 */
export default function ParentHomeScreen() {
  const theme = useTheme();

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.color.background.canvas }]}>
      <View style={styles.center}>
        <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>부모 홈</Text>
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
