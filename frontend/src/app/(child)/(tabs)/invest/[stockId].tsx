import { router, useLocalSearchParams } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { StockInfoCard, StockOverviewCard, TradeButtons, useStockDetail } from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Button, Header } from '@/shared/ui';

/**
 * 투자 상품 상세. 상세 응답 + 가격 이력(차트) + 보유 여부를 useStockDetail 이 묶어 주고,
 * 이 라우트는 배치와 사기/팔기 버튼만 담당한다.
 */
export default function InvestDetailScreen() {
  const theme = useTheme();
  const { stockId } = useLocalSearchParams<{ stockId: string }>();
  const { detail, isPending, isError, refetch } = useStockDetail(Number(stockId));

  return (
    <View style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}>
      <Header
        title="투자 상품 상세 보기"
        onBack={() => (router.canGoBack() ? router.back() : router.replace('/invest/index'))}
      />

      {detail ? (
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{
            paddingHorizontal: theme.layout.screenX,
            paddingVertical: theme.layout.screenY,
            gap: theme.space.xl,
          }}
        >
          <StockOverviewCard detail={detail} />
          <StockInfoCard detail={detail} />

          <TradeButtons detail={detail} />
        </ScrollView>
      ) : (
        <View style={[styles.empty, { gap: theme.space.lg }]}>
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
            {isPending ? '상품을 불러오는 중이에요.' : '상품을 불러오지 못했어요.'}
          </Text>
          {isError ? <Button label="다시 시도" variant="outline" onPress={() => refetch()} /> : null}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
