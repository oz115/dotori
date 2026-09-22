import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type SegmentedProgressBarProps = {
  /** 전체 칸 수 (적금은 납입 회차 = 주차). 1 미만이면 아무것도 그리지 않는다 */
  total: number;
  /** 채워진 칸 수 (지금까지 납입한 회차). total 로 clamp 된다 */
  filled: number;
};

/**
 * 눈금형 진행 막대. 연속 막대(ProgressBar)와 달리 기간을 칸으로 쪼개
 * "몇 주째 / 총 몇 주"를 한눈에 보여 준다 (적금: 1주 = 1칸).
 * 채운 칸은 brand.solid, 남은 칸은 트랙색으로 그린다.
 */
export function SegmentedProgressBar({ total, filled }: SegmentedProgressBarProps) {
  const theme = useTheme();
  if (total < 1) {
    return null;
  }
  const done = Math.max(0, Math.min(total, Math.round(filled)));

  return (
    <View
      style={styles.row}
      accessible
      accessibilityRole="progressbar"
      accessibilityValue={{ min: 0, max: total, now: done }}
    >
      {Array.from({ length: total }, (_, index) => (
        <View
          key={index}
          style={[
            styles.segment,
            {
              backgroundColor: index < done ? theme.color.brand.solid : theme.color.border.default,
            },
          ]}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    gap: 6,
  },
  segment: {
    flex: 1,
    height: 8,
    borderRadius: 999,
  },
});
