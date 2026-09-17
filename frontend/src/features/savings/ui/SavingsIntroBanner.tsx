import { Image, StyleSheet, Text, View } from 'react-native';

import { SAVINGS_BANNER_COPY, SAVINGS_BANNER_IMAGE } from '@/features/savings/lib/savingsAssets';
import type { SavingsGroup } from '@/features/savings/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  group: SavingsGroup;
};

/**
 * "예금이란?/적금이란?" 안내 배너 — 연녹색 카드에 설명과 저금통 캐릭터.
 * 본문에서 핵심 구절 하나를 brand 색으로 강조한다 (Figma: "차곡차곡").
 */
export function SavingsIntroBanner({ group }: Props) {
  const theme = useTheme();
  const copy = SAVINGS_BANNER_COPY[group];
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
