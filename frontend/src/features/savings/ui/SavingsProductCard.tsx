import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import type { SavingsCard } from '@/features/savings/model/types';
import { useTheme } from '@/shared/theme';
import { ProgressBar } from '@/shared/ui';

type Props = {
  card: SavingsCard;
  onPress?: () => void;
};

/**
 * 저축 메인의 상품 카드 (2열 그리드 한 칸) — 아이콘·상품명과 정보 줄들,
 * 가입 중이면 진행바까지. 만기 도달 건은 배지로 알려 수령을 유도한다.
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
          borderColor: theme.color.border.card,
          padding: theme.space.lg,
          gap: theme.space.md,
          opacity: pressed ? 0.85 : 1,
        },
      ]}
    >
      <View style={[styles.headRow, { gap: theme.space.sm }]}>
        <Image source={card.image} style={styles.icon} resizeMode="contain" />
        <Text
          numberOfLines={2}
          style={[theme.typography.label, styles.name, { color: theme.color.text.strong }]}
        >
          {card.name}
        </Text>
        <Ionicons name="chevron-forward" size={16} color={theme.color.text.muted} />
      </View>

      {card.matured ? (
        <View
          style={[
            styles.maturedBadge,
            { backgroundColor: theme.color.status.success.subtle, borderRadius: theme.radius.full },
          ]}
        >
          <Text style={[theme.typography.caption, { color: theme.color.status.success.text }]}>
            🎉 만기 달성! 받으러 가요
          </Text>
        </View>
      ) : null}

      <View style={{ gap: theme.space.xs }}>
        {card.lines.map((line) => (
          <View key={line.label} style={styles.lineRow}>
            <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>
              {line.label}
            </Text>
            <Text
              numberOfLines={1}
              style={[
                theme.typography.caption,
                styles.lineValue,
                { color: theme.color.text.strong },
              ]}
            >
              {line.value}
            </Text>
          </View>
        ))}
      </View>

      {card.progress != null ? <ProgressBar value={card.progress} /> : null}
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
    alignSelf: 'flex-start',
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
  lineRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 8,
  },
  lineValue: {
    flexShrink: 1,
  },
});
