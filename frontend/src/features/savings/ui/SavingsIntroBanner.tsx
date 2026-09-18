import { Image, StyleSheet, Text, View } from 'react-native';

import { SAVINGS_BANNER_COPY, SAVINGS_BANNER_IMAGE } from '@/features/savings/lib/savingsAssets';
import { useTheme } from '@/shared/theme';

/**
 * 저축 안내 배너 — 연녹색 카드에 설명과 저금통 캐릭터.
 * 본문에서 핵심 구절 하나를 brand 색으로 강조한다 (Figma: "차곡차곡").
 *
 * 예금·적금을 한 장으로 덮는다. 상단 선택이 "전체 · 예금 · 적금" 칩으로 바뀌면서 전체가
 * 기본이 됐고, 그 상태에서 한쪽만 설명하면 반쪽짜리 안내가 되기 때문이다.
 */
export function SavingsIntroBanner() {
  const theme = useTheme();
  const copy = SAVINGS_BANNER_COPY;
  const [before, after] = copy.body.split(copy.highlight);

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.xl,
          padding: theme.space.xl,
          gap: theme.space.md,
        },
      ]}
    >
      <View style={[styles.texts, { gap: theme.space.sm }]}>
        <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>{copy.title}</Text>
        <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
          {before}
          <Text style={{ color: theme.color.brand.solid, fontWeight: '700' }}>
            {copy.highlight}
          </Text>
          {after}
        </Text>
      </View>
      <Image source={SAVINGS_BANNER_IMAGE} style={styles.mascot} resizeMode="contain" />
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  texts: {
    flex: 1,
  },
  mascot: {
    width: 84,
    height: 96,
  },
});
