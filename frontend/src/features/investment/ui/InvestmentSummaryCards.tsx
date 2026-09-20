import { Image, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { changeColor } from '@/features/investment/lib/change';
import { formatAcorns } from '@/features/investment/lib/format';
import type { InvestmentSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

// 여우 마스코트(설명하기 포즈) — 투자 화면의 안내 캐릭터 (보유 화면의 다람쥐와 짝)
const FOX = require('@/assets/images/invest/fox.png'); // 400 x 424

/**
 * 지갑 잔액 표시 상태. 잔액은 wallet feature 가 소유하므로 호출하는 화면(라우트)이 조회해서 넘긴다.
 * 넘기지 않으면(prop 생략) 잔액 없이 보유 주식 요약만 보여준다.
 */
export type SummaryBalance = {
  /** 조회 전에는 undefined — "불러오는 중" 으로 표시한다 */
  amount?: number;
  failed?: boolean;
};

type Props = {
  summary: InvestmentSummary;
  balance?: SummaryBalance;
  mascot?: ImageSourcePropType;
};

/**
 * 투자 요약 카드 — 내 도토리(지갑 잔액)와, 지금 들고 있는 주식의 가치·산 값 대비 수익.
 * 수익은 아직 안 판 주식만 계산한다(이미 판 주식의 수익은 거래 내역에서 본다).
 * 가격은 하루에 한 번(자정 직후, 어제 뉴스 기준) 바뀌므로 그 사실도 함께 알려준다.
 * 산 주식이 없으면 숫자 대신 뉴스를 읽고 골라 보라는 안내만 보여준다.
 */
export function InvestmentSummaryCards({ summary, balance, mascot = FOX }: Props) {
  const theme = useTheme();
  const empty = summary.count === 0;

  const earned = summary.earnedPoints;
  const stockValue = summary.investedPoints + earned;
  const earnedColor = changeColor(earned, theme.color);
  const changeText =
    earned === 0
      ? '산 값과 같아요'
      : `산 값보다 ${formatAcorns(Math.abs(earned))} ${earned > 0 ? '올랐어요' : '내렸어요'}`;

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
          {balance ? (
            <>
              <Text style={sub}>내 도토리</Text>
              {balance.amount != null ? (
                <Text numberOfLines={1} adjustsFontSizeToFit style={[theme.typography.h1, strong]}>
                  {formatAcorns(balance.amount)}
                </Text>
              ) : (
                <Text style={[theme.typography.titleMd, strong]}>
                  {balance.failed ? '잔액을 불러오지 못했어요' : '불러오는 중이에요'}
                </Text>
              )}
            </>
          ) : empty ? (
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

      {balance ? (
        <View
          style={[
            styles.tile,
            {
              backgroundColor: theme.color.background.surface,
              borderColor: theme.color.border.default,
              borderRadius: theme.radius.lg,
              padding: theme.space.lg,
              gap: theme.space.sm,
            },
          ]}
        >
          {empty ? (
            <>
              <Text style={[theme.typography.label, strong]}>아직 산 주식이 없어요</Text>
              <Text style={sub}>뉴스를 읽고 오를 회사를 찾아보세요!</Text>
            </>
          ) : (
            <>
              <View style={styles.row}>
                <Text style={sub}>투자 중인 주식 {summary.count}개</Text>
                <Text style={[theme.typography.label, strong]}>{formatAcorns(stockValue)}</Text>
              </View>
              <View style={styles.row}>
                <Text style={sub}>수익</Text>
                <Text style={[theme.typography.label, { color: earnedColor }]}>{changeText}</Text>
              </View>
            </>
          )}
        </View>
      ) : empty ? (
        <Text style={sub}>뉴스를 읽고 오를 회사를 찾아보세요!</Text>
      ) : null}

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
  tile: {
    borderWidth: StyleSheet.hairlineWidth,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 8,
  },
});
