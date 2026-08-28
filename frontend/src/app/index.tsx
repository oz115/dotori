import { SafeAreaView, StyleSheet, Text, useColorScheme, View } from 'react-native';

import { CounterWidget } from '@/widgets/counter-widget';

/**
 * expo-router 라우트 = 화면. FSD-lite에서는 별도 pages 레이어를 두지 않고
 * 라우트 파일이 widgets/features를 직접 조합합니다. 다만 로직은 여기 두지 말고
 * widgets/features 로 내려보내고, 이 파일은 배치만 담당하세요.
 */
export default function HomeScreen() {
  const isDark = useColorScheme() === 'dark';

  return (
    <SafeAreaView style={[styles.safeArea, isDark && styles.safeAreaDark]}>
      <View style={styles.container}>
        <Text style={[styles.title, isDark && styles.titleDark]}>Dotori Town</Text>
        <Text style={[styles.description, isDark && styles.descriptionDark]}>
          FSD-lite + Expo Router 초기 세팅 예시 화면입니다.{'\n'}
          아래 카운터는 features/widgets 조합 예시이며, 실제 기능을 만들면서 지워도 됩니다.
        </Text>
        <CounterWidget />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  safeAreaDark: {
    backgroundColor: '#000000',
  },
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 24,
    paddingHorizontal: 24,
  },
  title: {
    color: '#111827',
    fontSize: 24,
    fontWeight: '700',
  },
  titleDark: {
    color: '#ffffff',
  },
  description: {
    color: '#6b7280',
    textAlign: 'center',
  },
  descriptionDark: {
    color: '#9ca3af',
  },
});
