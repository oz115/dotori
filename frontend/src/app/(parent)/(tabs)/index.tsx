import { router } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useFamilyStatus } from '@/features/family';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';
import { ParentHome } from '@/widgets/parent-home';

/**
 * 부모 홈. 가족이 아직 없으면 온보딩으로 보내는 안내만 띄우고,
 * 가족이 있으면 실제 홈(widgets/parent-home)을 그린다.
 */
export default function ParentHomeScreen() {
  const theme = useTheme();
  const status = useFamilyStatus();

  if (status.isPending) {
    return <View style={{ flex: 1, backgroundColor: theme.color.background.canvas }} />;
  }

  if (status.data?.hasFamily !== true) {
    return (
      <SafeAreaView
        edges={['top']}
        style={[styles.safe, { backgroundColor: theme.color.background.canvas }]}
      >
        <View style={styles.center}>
          <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>
            우리 가족을 만들어요
          </Text>
          <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
            가족을 만들고 아이를 초대하면 시작할 수 있어요.
          </Text>
          <Button
            label="우리 가족 만들기"
            variant="gradient"
            onPress={() => router.push('/onboarding')}
          />
        </View>
      </SafeAreaView>
    );
  }

  return <ParentHome />;
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
