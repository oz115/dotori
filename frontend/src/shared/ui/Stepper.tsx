import { StyleSheet, Text, View } from 'react-native';

import { fontFamily, useTheme } from '@/shared/theme';

type StepperProps = {
  /** 전체 단계 수 */
  total: number;
  /** 현재 단계 (1부터). 이 값 이하 단계는 완료로 채워진다 (누적). */
  current: number;
};

function useStepStyles() {
  const theme = useTheme();
  return (filled: boolean) => ({
    circle: {
      backgroundColor: filled ? theme.color.brand.solid : theme.color.border.default,
    },
    label: {
      color: filled ? theme.color.text.onBrand : theme.color.text.muted,
      fontFamily: fontFamily.bold,
    },
    connector: {
      backgroundColor: filled ? theme.color.brand.solid : theme.color.border.default,
    },
  });
}

const steps = (total: number) => Array.from({ length: total }, (_, i) => i + 1);

/**
 * 단계 배지 (Figma: "step"). 번호가 든 원을 나란히 놓는다.
 * 온보딩 "N단계" 표시처럼 연결선 없이 쓸 때 사용.
 */
export function Steps({ total, current }: StepperProps) {
  const stepStyle = useStepStyles();

  return (
    <View
      style={styles.stepsRow}
      accessibilityRole="progressbar"
      accessibilityValue={{ min: 1, max: total, now: current }}
    >
      {steps(total).map((n) => {
        const s = stepStyle(n <= current);
        return (
          <View key={n} style={[styles.circle, s.circle]}>
            <Text style={[styles.number, s.label]}>{n}</Text>
          </View>
        );
      })}
    </View>
  );
}

/**
 * 진행 표시기 (Figma: "Progress Indicator"). 단계 원을 가로 연결선으로 이어
 * 전체 흐름 속 현재 위치를 보여준다. 완료 단계와 그 사이 연결선까지 채운다.
 */
export function ProgressIndicator({ total, current }: StepperProps) {
  const stepStyle = useStepStyles();

  return (
    <View
      style={styles.progressRow}
      accessible
      accessibilityRole="progressbar"
      accessibilityValue={{ min: 1, max: total, now: current }}
    >
      {steps(total).map((n) => {
        const s = stepStyle(n <= current);
        return (
          <View key={n} style={[styles.progressItem, n > 1 && styles.progressItemGrow]}>
            {n > 1 ? <View style={[styles.connector, s.connector]} /> : null}
            <View style={[styles.circle, s.circle]}>
              <Text style={[styles.number, s.label]}>{n}</Text>
            </View>
          </View>
        );
      })}
    </View>
  );
}

const CIRCLE = 40;

const styles = StyleSheet.create({
  stepsRow: {
    flexDirection: 'row',
    gap: 12,
  },
  progressRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  progressItem: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  progressItemGrow: {
    flex: 1,
  },
  connector: {
    flex: 1,
    height: 2,
  },
  circle: {
    width: CIRCLE,
    height: CIRCLE,
    borderRadius: CIRCLE / 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  number: {
    fontSize: 15,
  },
});
