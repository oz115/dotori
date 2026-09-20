import { Fragment } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { changeColor, changeLabel } from '@/features/investment/lib/change';
import { formatAcorns, signedAcorns } from '@/features/investment/lib/format';
import type { Holding } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';
import { ContentCard } from '@/shared/ui';

type Props = {
  holdings: Holding[];
  onPressHolding: (holding: Holding) => void;
};

/**
 * 보유 종목을 카드 하나 안의 줄로 묶어 보여준다 — 요약 띠에서 종목마다 그림자 카드를 쌓으면
 * 시끄러워서, 한 카드 안에 구분선으로 잇는다. 종목당 1주 규칙이라 수량은 적지 않는다.
 * 줄마다 이름 아래에 산 값을 작게 두어 "얼마에 사서 지금 얼마인지"를 바로 읽는다.
 */
export function HoldingsListCard({ holdings, onPressHolding }: Props) {
  const theme = useTheme();

  return (
    <ContentCard style={{ paddingHorizontal: theme.space.lg }}>
      {holdings.map((holding, index) => {
        const earned = (holding.currentPrice - holding.buyPrice) * holding.quantity;
        const color = changeColor(earned, theme.color);

        return (
          <Fragment key={holding.id}>
            {index > 0 ? (
              <View style={[styles.divider, { backgroundColor: theme.color.border.default }]} />
            ) : null}
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${holding.name} 상세 보기`}
              onPress={() => onPressHolding(holding)}
              style={({ pressed }) => [
                styles.row,
                {
                  paddingVertical: theme.space.md,
                  gap: theme.space.md,
                  opacity: pressed ? 0.7 : 1,
                },
              ]}
            >
              {/* 아이콘 원본이 흰 배경이라 배경색 없이 둥근 모서리로만 자른다 */}
              <Image
                source={holding.image}
                style={[styles.icon, { borderRadius: theme.radius.md }]}
                resizeMode="cover"
              />
              <View style={styles.name}>
                <Text
                  numberOfLines={1}
                  style={[theme.typography.label, { color: theme.color.text.strong }]}
                >
                  {holding.name}
                </Text>
                <Text style={[theme.typography.caption, { color: theme.color.text.default }]}>
                  산 값 {formatAcorns(holding.buyPrice)}
                </Text>
              </View>
              <View style={styles.price} accessibilityLabel={changeLabel(earned)}>
                <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                  {formatAcorns(holding.currentPrice)}
                </Text>
                <Text style={[theme.typography.label, { color }]}>
                  {earned === 0 ? '변동 없음' : signedAcorns(earned)}
                </Text>
              </View>
            </Pressable>
          </Fragment>
        );
      })}
    </ContentCard>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center' },
  divider: { height: StyleSheet.hairlineWidth },
  icon: { width: 44, height: 44 },
  name: { flex: 1, gap: 2 },
  price: { alignItems: 'flex-end', gap: 2 },
});
