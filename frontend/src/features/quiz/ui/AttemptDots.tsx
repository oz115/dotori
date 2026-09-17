import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type AttemptDotsProps = {
  /** 채울 점 개수 (지금 도전 중인 회차) */
  filled: number;
  total: number;
};

/** "도전 기회 n/3" 옆에 붙는 작은 점 인디케이터. Stepper의 번호 원과는 다른, 장식용 표시라 따로 둔다. */
export function AttemptDots({ filled, total }: AttemptDotsProps) {
  const theme = useTheme();

  return (
    <View style={styles.row}>
      {Array.from({ length: total }, (_, i) => (
        <View
          key={i}
          style={[
            styles.dot,
            {
              backgroundColor: i < filled ? theme.color.brand.solid : theme.palette.neutral[500],
            },
          ]}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 6 },
  dot: { width: 8, height: 8, borderRadius: 4 },
});
