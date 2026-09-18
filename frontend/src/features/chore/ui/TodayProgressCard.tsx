import { StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';
import { ContentCard, ProgressBar } from '@/shared/ui';

type TodayProgressCardProps = {
  /** 오늘 지급까지 끝난 미션 수 */
  completed: number;
  /** 오늘 예정된 전체 미션 수 (완료·대기·거절 포함) */
  total: number;
};

/** 상단 배너 바로 아래 카드 — 오늘 미션을 몇 개 끝냈는지 막대로 보여준다. */
export function TodayProgressCard({ completed, total }: TodayProgressCardProps) {
  const theme = useTheme();
  const value = total > 0 ? completed / total : 0;

  return (
    <ContentCard style={{ padding: theme.space.lg, gap: theme.space.sm }}>
      <View style={styles.row}>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
          오늘 미션 진행률
        </Text>
        <Text style={[theme.typography.label, { color: theme.color.text.sub }]}>
          {completed} / {total}
        </Text>
      </View>
      <ProgressBar value={value} />
    </ContentCard>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
});
