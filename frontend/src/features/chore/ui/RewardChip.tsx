import { Image, StyleSheet, Text, View } from 'react-native';

import { isRewardReady } from '@/features/chore/lib/choreLabels';
import { useTheme } from '@/shared/theme';

type RewardChipProps = {
  rewardPoints: number | null;
  /** 목록처럼 좁은 자리에서는 "도토리"를 빼고 숫자만 보여 준다 (Figma 부모 목록) */
  compact?: boolean;
};

/**
 * 보상 칩. 도토리 아이콘 + "+500 도토리".
 *
 * rewardPoints 가 null 이면 AI 가 아직 산정 중이라 금액을 못 쓴다. 0원처럼 보이면
 * 부모가 보상을 안 준 것으로 오해하므로 "산정 중"을 따로 보여 준다.
 */
export function RewardChip({ rewardPoints, compact = false }: RewardChipProps) {
  const theme = useTheme();
  const ready = isRewardReady(rewardPoints);

  return (
    <View
      style={[
        styles.chip,
        {
          backgroundColor: ready ? theme.color.brand.subtle : theme.color.background.surfaceSunken,
          borderRadius: theme.radius.full,
          paddingVertical: theme.space.xs,
          paddingHorizontal: theme.space.md,
          gap: theme.space.xs,
        },
      ]}
    >
      {ready ? (
        <Image
          source={require('@/assets/images/chore/icon-acorn.png')}
          style={styles.acorn}
          resizeMode="contain"
        />
      ) : null}
      <Text
        style={[
          theme.typography.label,
          { color: ready ? theme.color.brand.subtleText : theme.color.text.muted },
        ]}
      >
        {ready ? `+ ${rewardPoints.toLocaleString('ko-KR')}${compact ? '' : ' 도토리'}` : '산정 중'}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  chip: { flexDirection: 'row', alignItems: 'center' },
  acorn: { width: 18, height: 18 },
});
