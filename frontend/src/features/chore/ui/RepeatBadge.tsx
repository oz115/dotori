import { StyleSheet, Text, View } from 'react-native';

import { repeatLabel } from '@/features/chore/lib/choreLabels';
import type { ChoreRepeatType } from '@/features/chore/model/types';
import { useTheme } from '@/shared/theme';

/** 반복 주기 배지 (한번·매일·매주·매월). 목록 행에서 제목 아래에 붙는다. */
export function RepeatBadge({ repeatType }: { repeatType: ChoreRepeatType }) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.badge,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.full,
          paddingHorizontal: theme.space.sm,
        },
      ]}
    >
      <Text style={[theme.typography.caption, { color: theme.color.brand.subtleText }]}>
        {repeatLabel(repeatType)}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { paddingVertical: 3 },
});
