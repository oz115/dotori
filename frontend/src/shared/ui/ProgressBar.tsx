import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type ProgressBarProps = {
  /** 진행률 0~1 */
  value: number;
};

/**
 * 단순 진행 막대. (전용 Figma 프레임이 없어 토큰 기준으로 구성 —
 * 트랙 border.default, 채움 brand.solid, 모서리 full.)
 */
export function ProgressBar({ value }: ProgressBarProps) {
  const theme = useTheme();
  const clamped = Math.max(0, Math.min(1, value));

  return (
    <View
      style={[styles.track, { backgroundColor: theme.color.border.default }]}
      accessible
      accessibilityRole="progressbar"
      accessibilityValue={{ min: 0, max: 100, now: Math.round(clamped * 100) }}
    >
      <View
        style={[
          styles.fill,
          { backgroundColor: theme.color.brand.solid, width: `${clamped * 100}%` },
        ]}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  track: {
    height: 8,
    borderRadius: 999,
    overflow: 'hidden',
  },
  fill: {
    height: '100%',
    borderRadius: 999,
  },
});
