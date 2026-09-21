import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type Props = {
  /** 가운데 글자 — 기간의 날짜 범위 (예: '2026.09.15 ~ 2026.09.21') */
  label: string;
  /** 더 오래된 거래가 있는 기간일 때만 true */
  canPrev: boolean;
  /** 지금(최근) 기간이 아닐 때만 true */
  canNext: boolean;
  onPrev: () => void;
  onNext: () => void;
};

/**
 * 기간 이동 막대 — 좌우 화살표로 한 기간씩 넘기고, 가운데에 달력 아이콘과 날짜 범위를 보여준다.
 * 생김새는 상점 구매내역의 월 이동 막대(‹ 📅 2026.09.01 ~ 2026.09.30 ›)와 같아서
 * 같은 동작이 화면마다 다르게 보이지 않는다. 다른 점은 한 달이 아니라 7일씩 움직인다는 것뿐이다.
 * 갈 곳이 없는 쪽 화살표는 흐리게 하고 눌리지 않게 한다 (거래가 없는 빈 기간으로 끝없이 가지 않도록).
 */
export function PeriodRangeBar({ label, canPrev, canNext, onPrev, onNext }: Props) {
  const theme = useTheme();

  return (
    <View style={[styles.bar, { gap: theme.space.lg }]}>
      <Arrow name="chevron-back" label="이전 기간" enabled={canPrev} onPress={onPrev} />
      <View style={styles.center}>
        <Ionicons name="calendar-outline" size={16} color={theme.color.text.muted} />
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>{label}</Text>
      </View>
      <Arrow name="chevron-forward" label="다음 기간" enabled={canNext} onPress={onNext} />
    </View>
  );
}

function Arrow({
  name,
  label,
  enabled,
  onPress,
}: {
  name: 'chevron-back' | 'chevron-forward';
  label: string;
  enabled: boolean;
  onPress: () => void;
}) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: !enabled }}
      disabled={!enabled}
      onPress={onPress}
      hitSlop={12}
    >
      <Ionicons
        name={name}
        size={20}
        color={enabled ? theme.color.text.default : theme.color.text.muted}
      />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  bar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  center: { flexDirection: 'row', alignItems: 'center', gap: 6 },
});
