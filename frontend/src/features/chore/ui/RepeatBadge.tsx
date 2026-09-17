import { StyleSheet, Text, View } from 'react-native';

import { repeatLabel } from '@/features/chore/lib/choreLabels';
import { repeatBadgeColors } from '@/features/chore/lib/repeatTheme';
import type { ChoreRepeatType } from '@/features/chore/model/types';
import { useTheme } from '@/shared/theme';

/**
 * 반복 주기 배지 (한번·매일·매주·매월). 목록 행에서 제목 위에 붙는다.
 *
 * 주기마다 색이 다르다 - 색은 repeatTheme 에 모여 있다.
 */
export function RepeatBadge({ repeatType }: { repeatType: ChoreRepeatType }) {
  const theme = useTheme();
  const color = repeatBadgeColors(theme, repeatType);

  return (
    <View
      style={[
        styles.badge,
        {
          backgroundColor: color.bg,
          borderRadius: theme.radius.full,
          paddingHorizontal: theme.space.sm,
        },
      ]}
    >
      <Text style={[theme.typography.caption, { color: color.text }]}>
        {repeatLabel(repeatType)}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { paddingVertical: 3 },
});
