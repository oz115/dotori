import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { InvestmentNews } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

/**
 * 오늘 뉴스 세 건을 한 번에 훑어볼 수 있는 카드 목록. 카드를 탭하면 본문 페이지로 넘어간다.
 * 어떤 회사에 영향을 주는지는 알려주지 않는다 — 아이가 직접 생각해 보는 것이 목적이다.
 */
export function NewsList({
  items,
  onPressItem,
}: {
  items: InvestmentNews[];
  onPressItem: (item: InvestmentNews) => void;
}) {
  const theme = useTheme();
  return (
    <View style={{ gap: theme.space.md }}>
      {items.map((item) => (
        <Pressable
          key={item.id}
          accessibilityRole="button"
          accessibilityLabel={`${item.title} 뉴스 읽기`}
          onPress={() => onPressItem(item)}
          style={({ pressed }) => [
            styles.card,
            {
              backgroundColor: theme.color.background.surfaceSunken,
              borderRadius: theme.radius.xl,
              padding: theme.space.xl,
              gap: theme.space.md,
              opacity: pressed ? 0.85 : 1,
            },
          ]}
        >
          <View style={[styles.texts, { gap: theme.space.sm }]}>
            <View
              style={[
                styles.badge,
                { backgroundColor: theme.color.brand.solid, borderRadius: theme.radius.full },
              ]}
            >
              <Text style={[theme.typography.caption, { color: theme.color.text.onBrand }]}>
                {item.tag}
              </Text>
            </View>
            <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
              {item.title}
            </Text>
          </View>
          <Text style={styles.illustration}>{item.emoji}</Text>
        </Pressable>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    overflow: 'hidden',
  },
  texts: {
    flex: 1,
    alignItems: 'flex-start',
  },
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 3,
  },
  illustration: {
    fontSize: 44,
  },
});
