import { StyleSheet, Text, useColorScheme, View } from 'react-native';

import { IncrementButton, useCounterStore } from '@/features/counter';

/**
 * widgets 레이어 예시.
 * features/counter 하나(값 읽기 + 증가 버튼)를 화면에 배치 가능한 블록으로 묶습니다.
 * 여러 feature를 조합할 때는 이 레이어에서 조합합니다.
 */
export function CounterWidget() {
  const count = useCounterStore((state) => state.count);
  const isDark = useColorScheme() === 'dark';

  return (
    <View style={[styles.container, isDark && styles.containerDark]}>
      <Text style={[styles.label, isDark && styles.labelDark]}>count</Text>
      <Text style={[styles.count, isDark && styles.countDark]}>{count}</Text>
      <IncrementButton />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    gap: 16,
    borderRadius: 16,
    backgroundColor: '#f3f4f6',
    padding: 24,
  },
  containerDark: {
    backgroundColor: '#1f2937',
  },
  label: {
    color: '#4b5563',
    fontSize: 18,
  },
  labelDark: {
    color: '#d1d5db',
  },
  count: {
    color: '#111827',
    fontSize: 36,
    fontWeight: '700',
  },
  countDark: {
    color: '#ffffff',
  },
});
