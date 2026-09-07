import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useTheme } from '@/shared/theme';

/**
 * (auth) 그룹 진입 화면. 로그인 UI 는 다음 태스크(로그인 화면)에서 구현한다.
 */
export default function AuthEntryScreen() {
  const theme = useTheme();

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.color.background.canvas }]}>
      <View style={styles.center}>
        <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>도토리</Text>
        <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
          로그인 화면 준비 중
        </Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 8 },
});
