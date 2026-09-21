import { Pressable, StyleSheet, Text, View } from 'react-native';

import { formatWeekLabel, isCurrentWeek } from '@/features/report/lib/reportPeriod';
import type { ReportPeriod } from '@/features/report/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  period: ReportPeriod;
  onChange: (weeks: number) => void;
};

/**
 * 주 단위 기간 이동. `‹ 9/14 - 9/20 (이번 주) ›`
 *
 * <p>프리셋 칩(이번 달·지난 달) 대신 앞뒤 이동을 쓴다 — 주 단위는 "몇 주 전"을 세는 것이
 * 아니라 하나씩 되짚어 보는 쪽이 자연스럽다.
 *
 * <p><b>다음 주로는 못 넘어간다.</b> 아직 오지 않은 주는 전부 빈 화면이라 보여 줄 것이 없고,
 * 넘어갈 수 있으면 부모가 "데이터가 안 나온다"로 읽는다.
 */
export function WeekStepper({ period, onChange }: Props) {
  const theme = useTheme();
  const atCurrentWeek = isCurrentWeek(period);

  return (
    <View style={styles.row}>
      <StepButton label="‹" onPress={() => onChange(-1)} />

      <View style={[styles.labelBox, { gap: theme.space.xs }]}>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
          {formatWeekLabel(period)}
        </Text>
        {atCurrentWeek ? (
          <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>이번 주</Text>
        ) : null}
      </View>

      <StepButton label="›" onPress={() => onChange(1)} disabled={atCurrentWeek} />
    </View>
  );
}

function StepButton({
  label,
  onPress,
  disabled = false,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
}) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      hitSlop={8}
      accessibilityRole="button"
      accessibilityState={{ disabled }}
      accessibilityLabel={label === '‹' ? '지난 주' : '다음 주'}
      style={styles.stepButton}
    >
      <Text
        style={[
          theme.typography.h3,
          { color: disabled ? theme.color.text.muted : theme.color.text.strong },
        ]}
      >
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  labelBox: { flexDirection: 'row', alignItems: 'baseline' },
  // 화살표는 글자가 작아서 터치 영역을 따로 넓힌다 (hitSlop 과 함께)
  stepButton: { minWidth: 44, alignItems: 'center' },
});
