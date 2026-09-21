import { Image, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { changeColor, sinceBoughtLabel } from '@/features/investment/lib/change';
import { formatAcorns } from '@/features/investment/lib/format';
import type { InvestmentSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

// 여우 마스코트(설명하기 포즈) — 투자 화면의 안내 캐릭터 (보유 화면의 다람쥐와 짝)
const FOX = require('@/assets/images/invest/fox.png'); // 400 x 424

type Props = {
  summary: InvestmentSummary;
  mascot?: ImageSourcePropType;
};

/**
 * 투자 요약 카드 — 지금 들고 있는 주식의 가치(보유 가치)와 산 값 대비 수익(순이익).
 * 지갑 잔액(포인트)은 여기서 보여주지 않는다 — 투자 화면의 주인공은 주식이고,
 * 잔액은 홈·지갑에서 본다는 팀 결정이다.
 * 수익은 아직 안 판 주식만 계산한다(이미 판 주식의 수익은 거래 내역에서 본다).
 * 가격은 하루에 한 번(자정 직후, 어제 뉴스 기준) 바뀌므로 그 사실도 함께 알려준다.
 * 산 주식이 없으면 숫자 대신 뉴스를 읽고 골라 보라는 안내만 보여준다.
 */
export function InvestmentSummaryCards({ summary, mascot = FOX }: Props) {
  const theme = useTheme();
  const empty = summary.count === 0;

  const earned = summary.earnedPoints;
  const stockValue = summary.investedPoints + earned;
  const earnedColor = changeColor(earned, theme.color);
  const changeText = sinceBoughtLabel(earned);

  const sub = [theme.typography.bodySm, { color: theme.color.text.sub }];
  const strong = { color: theme.color.text.strong };

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
          {empty ? (
            <Text style={[theme.typography.titleMd, strong]}>아직 산 주식이 없어요</Text>
          ) : (
            <>
              <Text style={sub}>내 주식 {summary.count}개의 지금 가치</Text>
              <Text numberOfLines={1} adjustsFontSizeToFit style={[theme.typography.h2, strong]}>
                {formatAcorns(stockValue)}
              </Text>
              <Text style={[theme.typography.label, { color: earnedColor }]}>{changeText}</Text>
            </>
          )}
        </View>
        <Image source={mascot} style={styles.mascot} resizeMode="contain" />
      </View>

      {empty ? <Text style={sub}>뉴스를 읽고 오를 회사를 찾아보세요!</Text> : null}

      <Text style={sub}>가격은 하루에 한 번, 자정 직후에 바뀌어요.</Text>
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
});
