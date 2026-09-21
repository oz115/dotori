import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import type { SavingsCard } from '@/features/savings/model/types';
import { useTheme } from '@/shared/theme';
import { SegmentedProgressBar } from '@/shared/ui';

type Props = {
  card: SavingsCard;
  onPress?: () => void;
};

/**
 * 저축 메인의 상품 카드 — 주식 리스트처럼 가로로 긴 한 줄 카드를 세로로 쌓는다.
 * 상단에 아이콘·상품명, 아래에 정보 값들을 가로로 펼치고, 가입 중이면 진행바까지.
 * 만기 도달 건은 배지로 알려 수령을 유도한다.
 */
export function SavingsProductCard({ card, onPress }: Props) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${card.name} 상세 보기`}
      onPress={onPress}
      style={({ pressed }) => [
        styles.card,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderRadius: theme.radius.xl,
          // 가입 중인 상품은 초록 테두리로 강조해 목록에서 '내 저축'을 바로 찾게 한다
          borderColor: card.subscribed ? theme.color.brand.solid : theme.color.border.card,
          borderWidth: card.subscribed ? 1.5 : 1,
          padding: theme.space.lg,
          gap: theme.space.md,
          opacity: pressed ? 0.85 : 1,
        },
      ]}
    >
      <View style={[styles.headRow, { gap: theme.space.sm }]}>
        <Image source={card.image} style={styles.icon} resizeMode="contain" />
        <Text
          numberOfLines={1}
          style={[theme.typography.label, styles.name, { color: theme.color.text.strong }]}
        >
          {card.name}
        </Text>
        {card.matured ? (
          <View
            style={[
              styles.maturedBadge,
              {
                backgroundColor: theme.color.status.success.subtle,
                borderRadius: theme.radius.full,
              },
            ]}
          >
            <Text style={[theme.typography.caption, { color: theme.color.status.success.text }]}>
              🎉 만기 달성!
            </Text>
          </View>
        ) : null}
        <Ionicons name="chevron-forward" size={16} color={theme.color.text.muted} />
      </View>

      {/* 정보 값들을 가로로 펼친다 — 카드가 화면 너비를 다 쓰므로 세로 줄보다 눈에 잘 들어온다 */}
      <View style={styles.statsRow}>
        {card.lines.map((line) => (
          <View key={line.label} style={styles.stat}>
            <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>
              {line.label}
            </Text>
            <Text
              numberOfLines={1}
              style={[theme.typography.label, { color: theme.color.text.strong }]}
            >
              {line.value}
            </Text>
          </View>
        ))}
      </View>

      {card.segments != null ? (
        <SegmentedProgressBar total={card.segments.total} filled={card.segments.filled} />
      ) : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  // 상점 상품 카드와 같은 문법 — 뚜렷한 1px 테두리 + 옅은 그림자
  card: {
    borderWidth: 1,
  },
  headRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  icon: {
    width: 40,
    height: 40,
  },
  name: {
    flex: 1,
  },
  maturedBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
  statsRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  stat: {
    flex: 1,
    gap: 4,
  },
});
