import { Image, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { changeColor } from '@/features/investment/lib/change';
import { formatAcorns, signedAcorns } from '@/features/investment/lib/format';
import type { InvestmentSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

// 여우 마스코트(설명하기 포즈) — 투자 화면의 안내 캐릭터 (보유 화면의 다람쥐와 짝)
const FOX = require('@/assets/images/invest/fox.png'); // 400 x 424

type Props = {
  summary: InvestmentSummary;
  mascot?: ImageSourcePropType;
};

/**
 * 투자 요약 배너 — 산 값·지금 가치·수익을 나란한 세 칸으로 보여준다.
 *
 * <p>전에는 "지금 가치" 한 숫자와 "산 값보다 얼마 올랐어요" 문장뿐이라, 얼마를 넣어서
 * 얼마가 됐는지를 한눈에 못 봤다 (QA). 세 숫자를 같은 크기 칸에 나란히 두면 "넣은 값 →
 * 지금 값 → 그 차이" 가 왼쪽에서 오른쪽으로 읽힌다.
 *
 * <p>지갑 잔액(포인트)은 여기서 보여주지 않는다 — 투자 화면의 주인공은 주식이고, 잔액은
 * 홈·지갑에서 본다는 팀 결정이다. 수익은 아직 안 판 주식만 계산한다(판 주식의 수익은
 * 거래 내역에서 본다).
 *
 * <p>산 주식이 없으면 숫자 칸 대신 뉴스를 읽고 골라 보라는 안내만 보여준다.
 */
export function InvestmentSummaryCards({ summary, mascot = FOX }: Props) {
  const theme = useTheme();
  const empty = summary.count === 0;

  const earned = summary.earnedPoints;
  const stockValue = summary.investedPoints + earned;

  const sub = [theme.typography.bodySm, { color: theme.color.text.sub }];

  return (
    <View
      style={{
        backgroundColor: theme.color.brand.subtle,
        borderRadius: theme.radius.xl,
        padding: theme.space.xl,
        gap: theme.space.lg,
      }}
    >
      <View style={styles.headRow}>
        <View style={[styles.headTexts, { gap: theme.space.xs }]}>
          {/* "내 주식 N개"는 짧은 한 줄이라 마스코트 높이만큼 옆이 비어 보인다 — 크게 띄운다.
              빈 상태 문구는 길고 아래 안내 줄도 있어 원래 크기를 유지한다 */}
          <Text
            style={[
              empty ? theme.typography.titleMd : theme.typography.h2,
              { color: theme.color.text.strong },
            ]}
          >
            {empty ? '아직 산 주식이 없어요' : `내 주식 ${summary.count}개`}
          </Text>
          {empty ? <Text style={sub}>뉴스를 읽고 오를 회사를 찾아보세요!</Text> : null}
        </View>
        <Image source={mascot} style={styles.mascot} resizeMode="contain" />
      </View>

      {empty ? null : (
        <View style={[styles.boxRow, { gap: theme.space.sm }]}>
          <SummaryBox label="산 값" value={formatAcorns(summary.investedPoints)} />
          <SummaryBox label="지금 가치" value={formatAcorns(stockValue)} />
          <SummaryBox
            label="수익"
            value={earned === 0 ? formatAcorns(0) : signedAcorns(earned)}
            valueColor={changeColor(earned, theme.color)}
          />
        </View>
      )}

      <Text style={sub}>⏰ 가격은 하루에 한 번, 자정 직후에 바뀌어요.</Text>
    </View>
  );
}

/**
 * 요약 한 칸. 세 칸이 같은 폭이라 금액 자릿수가 달라도 기준선이 흔들리지 않는다.
 * 숫자는 칸을 넘치면 줄여서라도 한 줄에 넣는다 — 줄바꿈되면 칸마다 높이가 달라진다.
 */
function SummaryBox({
  label,
  value,
  valueColor,
}: {
  label: string;
  value: string;
  valueColor?: string;
}) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.box,
        {
          backgroundColor: theme.color.background.surface,
          borderRadius: theme.radius.lg,
          paddingVertical: theme.space.md,
          paddingHorizontal: theme.space.sm,
          gap: theme.space.xs,
        },
      ]}
    >
      <Text style={[theme.typography.caption, styles.center, { color: theme.color.text.muted }]}>
        {label}
      </Text>
      <Text
        numberOfLines={1}
        adjustsFontSizeToFit
        style={[
          theme.typography.label,
          styles.center,
          { color: valueColor ?? theme.color.text.strong },
        ]}
      >
        {value}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  headRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headTexts: {
    flex: 1,
    paddingRight: 8,
  },
  mascot: {
    width: 84,
    height: 89,
  },
  boxRow: {
    flexDirection: 'row',
  },
  box: {
    flex: 1,
  },
  center: {
    textAlign: 'center',
  },
});
