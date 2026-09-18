import { Fragment } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';

import {
  SAVINGS_BANNER_COPY,
  SAVINGS_BANNER_IMAGE,
  savingsBannerSegments,
} from '@/features/savings/lib/savingsAssets';
import { useTheme } from '@/shared/theme';

/**
 * 저축 안내 배너 — 연녹색 카드에 설명과 저금통 캐릭터.
 *
 * 예금·적금을 한 장으로 덮는다. 상단 선택이 "전체 · 예금 · 적금" 칩으로 바뀌면서 전체가
 * 기본이 됐고, 그 상태에서 한쪽만 설명하면 반쪽짜리 안내가 되기 때문이다.
 *
 * <p>본문에서 두 구절("한 번에 맡겨서" · "차곡차곡")을 brand 색으로 강조한다. 쪼개는
 * 일은 문구를 가진 쪽(savingsBannerSegments)이 하고 여기서는 색만 입힌다.
 */
export function SavingsIntroBanner() {
  const theme = useTheme();
  const copy = SAVINGS_BANNER_COPY;

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
          {savingsBannerSegments(copy).map((segment, index) => (
            <Fragment key={index}>
              {segment.emphasis ? (
                <Text style={{ color: theme.color.brand.solid, fontWeight: '700' }}>
                  {segment.text}
                </Text>
              ) : (
                segment.text
              )}
            </Fragment>
          ))}
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
