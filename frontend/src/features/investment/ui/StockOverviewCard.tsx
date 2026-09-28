import { Image, StyleSheet, Text, View } from 'react-native';

import { changeColor } from '@/features/investment/lib/change';
import { formatNumber, signedAcorns } from '@/features/investment/lib/format';
import type { StockDetail } from '@/features/investment/model/types';
import { PriceLineChart } from '@/features/investment/ui/PriceLineChart';
import { Button } from '@/shared/ui';
import { useTheme } from '@/shared/theme';

type Props = {
  detail: StockDetail;
  historyPending?: boolean;
  historyError?: boolean;
  historyFetching?: boolean;
  onRetryHistory?: () => void;
  /**
   * "내가 산 값 … 차익 …" 줄을 보여줄지. 자녀 화면은 기본값(true).
   *
   * 부모 화면에서는 끈다 — 그 매수가는 부모 것이 아니라 아이 것이라 "내가 산 값" 이
   * 틀린 말이고, 부모는 사고팔 수 없어 차익을 여기서 볼 이유도 없다 (QA).
   */
  showMyPosition?: boolean;
};

/**
 * 상세 화면 상단 개요 카드 — 상품 이름·배지(카테고리/보유중)·현재가·어제 대비·가격 차트.
 */
export function StockOverviewCard({
  detail,
  historyPending,
  historyError,
  historyFetching,
  onRetryHistory,
  showMyPosition = true,
}: Props) {
  const theme = useTheme();

  const color = changeColor(detail.changeFromYesterday, theme.color);

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.xl,
          padding: theme.space.xl,
          gap: theme.space.lg,
        },
      ]}
    >
      <View style={styles.topRow}>
        <View style={[styles.nameBlock, { gap: theme.space.md }]}>
          {/* 아이콘 원본이 흰 배경이라 연녹색 카드 위에서 흰 둥근 타일로 보이게 자른다 */}
          <Image
            source={detail.image}
            style={[styles.icon, { borderRadius: theme.radius.lg }]}
            resizeMode="cover"
          />
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
          <Text style={{ color }}>
            {detail.changeFromYesterday === 0
              ? '변동 없음'
              : signedAcorns(detail.changeFromYesterday)}
          </Text>
        </Text>
        {/* 보유 중이면 산 값 대비 차익을 수치로 — "같아요" 같은 말 대신 숫자를 보여준다 */}
        {showMyPosition && detail.holding && detail.buyPrice != null ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            내가 산 값 {formatNumber(detail.buyPrice)} 도토리 · 차익{' '}
            <Text
              style={{ color: changeColor(detail.currentPrice - detail.buyPrice, theme.color) }}
            >
              {detail.currentPrice === detail.buyPrice
                ? '0 도토리'
                : signedAcorns(detail.currentPrice - detail.buyPrice)}
            </Text>
          </Text>
        ) : null}
      </View>

      {historyError ? (
        <View style={{ gap: theme.space.sm }}>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
            가격 기록을 불러오지 못했어요.
          </Text>
          <Button
            label="다시 불러오기"
            variant="outline"
            loading={historyFetching}
            onPress={onRetryHistory}
          />
        </View>
      ) : historyPending ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
          가격 기록을 불러오는 중이에요.
        </Text>
      ) : (
        <PriceLineChart history={detail.history} />
      )}
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
