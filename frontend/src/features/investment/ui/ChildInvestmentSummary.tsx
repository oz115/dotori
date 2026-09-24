import { Ionicons } from '@expo/vector-icons';
import { Image, StyleSheet, Text, View } from 'react-native';

import { changeColor, sinceBoughtLabel } from '@/features/investment/lib/change';
import { formatAcorns, profitLabel } from '@/features/investment/lib/format';
import type { RealizedProfitSummary } from '@/features/investment/lib/transactions';
import type { InvestmentSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';
import { ContentCard } from '@/shared/ui';

// 판매로 확정된 수익 줄의 아이콘 — 위 제목의 캔들 아이콘과 겹치지 않게 다른 것을 쓴다
const COIN = require('@/assets/images/invest/coin.png');

/** 최근 N일 판매 수익 — 거래 내역은 보유와 따로 조회하므로 아직 못 받았거나 실패한 상태가 있다. */
export type RecentSalesProfit =
  { status: 'pending' } | { status: 'error' } | ({ status: 'ready' } & RealizedProfitSummary);

type Props = {
  summary: InvestmentSummary;
  sales: RecentSalesProfit;
  /** 판매 수익을 셀 기간(일). "최근 7일 판매 수익" 라벨에 쓴다 */
  days: number;
};

/**
 * 부모 투자 화면의 자녀 현황 카드 — 지금 들고 있는 주식의 가치를 크게 보여주고,
 * 산 값보다 올랐는지 내렸는지를 색 알약(화살표 + 문구)으로 한눈에 알려준다.
 * 그 아래에 최근 N일 동안 팔아서 확정된 수익을 한 줄로 붙인다.
 *
 * 아직 안 판 주식(보유 수익)과 이미 판 주식(판매 수익)은 성격이 달라 한 숫자로 합치지 않고
 * 줄을 나눴다. 자녀 카드(InvestmentSummaryCards)와 문구·색 규칙은 같아서 부모와 아이가
 * 같은 숫자를 다른 말로 부르지 않는다. 색만으로 구분하지 않도록 알약에 화살표와 문구를 함께 쓴다.
 */
export function ChildInvestmentSummary({ summary, sales, days }: Props) {
  const theme = useTheme();
  const earned = summary.earnedPoints;
  const strong = { color: theme.color.text.strong };
  const body = [theme.typography.label, { color: theme.color.text.default }];

  // 등락 알약 — 글자색은 투자 색 규칙(changeColor), 바탕은 같은 계열의 옅은 색
  const tone = earned > 0 ? 'rise' : earned < 0 ? 'fall' : 'flat';
  const pill = {
    rise: { bg: theme.color.status.info.subtle, icon: 'caret-up' as const },
    fall: { bg: theme.color.status.danger.subtle, icon: 'caret-down' as const },
    flat: { bg: theme.color.background.surfaceSunken, icon: 'remove' as const },
  }[tone];
  const pillColor = changeColor(earned, theme.color);

  return (
    <ContentCard style={{ padding: theme.space.xl, gap: theme.space.lg }}>
      {summary.count === 0 ? (
        <View style={{ gap: theme.space.xs }}>
          <Text style={[theme.typography.titleMd, strong]}>아직 산 주식이 없어요</Text>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.default }]}>
            주식을 사면 어떤 주식을 들고 있는지 여기서 볼 수 있어요.
          </Text>
        </View>
      ) : (
        <View style={{ gap: theme.space.sm }}>
          <Text style={body}>들고 있는 주식 {summary.count}개의 지금 가치</Text>
          <Text numberOfLines={1} adjustsFontSizeToFit style={[theme.typography.h1, strong]}>
            {formatAcorns(summary.investedPoints + earned)}
          </Text>
          <View
            style={[
              styles.pill,
              {
                backgroundColor: pill.bg,
                borderRadius: theme.radius.full,
                paddingHorizontal: theme.space.md,
                paddingVertical: theme.space.xs,
                gap: theme.space.xs,
              },
            ]}
          >
            <Ionicons name={pill.icon} size={16} color={pillColor} />
            <Text style={[theme.typography.label, { color: pillColor }]}>
              {sinceBoughtLabel(earned)}
            </Text>
          </View>
        </View>
      )}

      <View style={[styles.divider, { backgroundColor: theme.color.border.default }]} />

      <View style={styles.row}>
        <View style={[styles.rowLabel, { gap: theme.space.sm }]}>
          <Image source={COIN} style={styles.coin} resizeMode="contain" />
          <Text style={body}>최근 {days}일 판매 수익</Text>
        </View>
        <SalesProfitText sales={sales} />
      </View>
    </ContentCard>
  );
}

function SalesProfitText({ sales }: { sales: RecentSalesProfit }) {
  const theme = useTheme();
  const muted = [theme.typography.bodySm, { color: theme.color.text.default }];

  if (sales.status === 'pending') return <Text style={muted}>불러오는 중이에요</Text>;
  if (sales.status === 'error') return <Text style={muted}>불러오지 못했어요</Text>;
  if (sales.count === 0) return <Text style={muted}>판 주식이 없어요</Text>;
  // 원가를 모르는 판매가 섞이면 합계가 틀리므로 확정값처럼 보여주지 않는다 (판매 수익 카드와 같은 규칙)
  if (sales.incomplete) return <Text style={muted}>계산할 정보가 부족해요</Text>;

  return (
    <Text style={[theme.typography.titleMd, { color: changeColor(sales.total, theme.color) }]}>
      {profitLabel(sales.total)}
    </Text>
  );
}

const styles = StyleSheet.create({
  pill: { alignSelf: 'flex-start', flexDirection: 'row', alignItems: 'center' },
  divider: { height: StyleSheet.hairlineWidth },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  rowLabel: { flexDirection: 'row', alignItems: 'center', flexShrink: 1 },
  coin: { width: 28, height: 28 },
});
