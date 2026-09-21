import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { savingsGroupLabel } from '@/features/savings/lib/savingsView';
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
        <View style={[styles.nameGroup, { gap: theme.space.xs }]}>
          <Text
            numberOfLines={1}
            style={[theme.typography.label, styles.name, { color: theme.color.text.strong }]}
          >
            {card.name}
          </Text>
          {/* 상품명 옆 카테고리 배지 — 종류별 색으로 목록에서 바로 구분하게 한다.
              브랜드 그린(가입 강조 테두리)과 겹치지 않게 예금=파랑, 적금=앰버,
              성격이 다른 보통예금(자유입출금)은 도토리 브라운으로 따로 뺀다 */}
          {(() => {
            const badge = savingsBadge(card, theme);
            return (
              <View
                style={[
                  styles.groupBadge,
                  { backgroundColor: badge.bg, borderRadius: theme.radius.full },
                ]}
              >
                <Text style={[theme.typography.caption, { color: badge.text }]}>{badge.label}</Text>
              </View>
            );
          })()}
        </View>
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

/**
 * 카드 카테고리 배지의 라벨·색. 예금=파랑, 적금=앰버로 나누되, 자유입출금(보통예금)은
 * 예금과 성격이 달라 도토리 브라운으로 따로 뺀다. 브랜드 그린(가입 강조)과는 겹치지 않는다.
 */
function savingsBadge(
  card: SavingsCard,
  theme: ReturnType<typeof useTheme>,
): { label: string; bg: string; text: string } {
  if (card.productType === 'ORDINARY') {
    // green.50 40% 인 brand.tint 와 같은 방식 — 도토리 브라운을 옅게 깐다
    return { label: '자유입출금', bg: 'rgba(130, 84, 48, 0.12)', text: theme.color.accent.acorn };
  }
  if (card.group === 'DEPOSIT') {
    return {
      label: savingsGroupLabel(card.group),
      bg: theme.color.status.info.subtle,
      text: theme.color.status.info.text,
    };
  }
  return {
    label: savingsGroupLabel(card.group),
    bg: theme.color.status.warning.subtle,
    text: theme.color.status.warning.text,
  };
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
  nameGroup: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  name: {
    flexShrink: 1,
  },
  groupBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
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
