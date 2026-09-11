import { StyleSheet, Text, View } from 'react-native';

import { formatNumber, signedAcorns } from '@/features/investment/lib/format';
import type { StockDetail } from '@/features/investment/model/types';
import { PriceLineChart } from '@/features/investment/ui/PriceLineChart';
import { useTheme } from '@/shared/theme';

type Props = {
  detail: StockDetail;
};

/**
 * 상세 화면 상단 개요 카드 — 상품 이름·배지(카테고리/보유중)·현재가·어제 대비·가격 차트.
 */
export function StockOverviewCard({ detail }: Props) {
  const theme = useTheme();

  const rising = detail.changeFromYesterday >= 0;
  const changeColor = rising ? theme.color.status.success.text : theme.color.status.danger.text;

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: theme.color.brand.tint,
          borderRadius: theme.radius.xl,
          padding: theme.space.xl,
          gap: theme.space.lg,
        },
      ]}
    >
      <View style={styles.topRow}>
        <View style={[styles.nameBlock, { gap: theme.space.md }]}>
          {/* 상품 일러스트 자리 표시자 — 상품용 에셋이 생기면 이미지로 교체 */}
          <View
            style={[
              styles.icon,
              { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.lg },
            ]}
          >
            <Text style={styles.iconEmoji}>{detail.emoji}</Text>
          </View>
          <Text
            numberOfLines={1}
            style={[theme.typography.h3, styles.name, { color: theme.color.text.strong }]}
          >
            {detail.name}
          </Text>
        </View>

        <View style={[styles.badges, { gap: theme.space.sm }]}>
          <View
            style={[
              styles.badge,
              {
                backgroundColor: theme.color.background.surface,
                borderColor: theme.color.brand.border,
                borderRadius: theme.radius.full,
              },
            ]}
          >
            <Text style={[theme.typography.caption, { color: theme.color.brand.subtleText }]}>
              {detail.category}
            </Text>
          </View>
          {detail.holding ? (
            <View
              style={[
                styles.badge,
                styles.holdingBadge,
                { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.full },
              ]}
            >
              <Text style={[theme.typography.caption, { color: theme.color.brand.subtleText }]}>
                🌱 보유중
              </Text>
            </View>
          ) : null}
        </View>
      </View>

      <View style={{ gap: theme.space.xs }}>
        <View style={styles.priceRow}>
          <Text style={[theme.typography.h1, { color: theme.color.text.strong }]}>
            {formatNumber(detail.currentPrice)}
          </Text>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>도토리</Text>
        </View>
        <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
          어제보다{' '}
          <Text style={{ color: changeColor }}>{signedAcorns(detail.changeFromYesterday)}</Text>
        </Text>
      </View>

      <PriceLineChart history={detail.history} />
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    overflow: 'hidden',
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
  },
  nameBlock: {
    flexDirection: 'row',
    alignItems: 'center',
    flexShrink: 1,
  },
  icon: {
    width: 52,
    height: 52,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconEmoji: {
    fontSize: 30,
  },
  name: {
    flexShrink: 1,
  },
  badges: {
    alignItems: 'flex-end',
  },
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  holdingBadge: {
    borderWidth: 0,
  },
  priceRow: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: 6,
  },
});
