import { useQueryClient } from '@tanstack/react-query';
import { walletKeys } from '@/features/wallet';
import { router, useLocalSearchParams } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import {
  StockInfoCard,
  StockOverviewCard,
  TradeButtons,
  useStockDetail,
} from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Button, Header, Screen } from '@/shared/ui';

/**
 * 투자 상품 상세. 상세 응답 + 가격 이력(차트) + 보유 여부를 useStockDetail 이 묶어 주고,
 * 이 라우트는 배치와 사기/팔기 버튼만 담당한다.
 */
export default function InvestDetailScreen() {
  const theme = useTheme();
  const queryClient = useQueryClient();
  const { stockId } = useLocalSearchParams<{ stockId: string }>();
  const {
    detail,
    isPending,
    isError,
    refetch,
    historyPending,
    historyError,
    historyFetching,
    refetchHistory,
  } = useStockDetail(Number(stockId));

  return (
    <Screen
      header={
        <Header
          title="투자 상품 상세 보기"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/invest/index'))}
        />
      }
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
    >
      {detail ? (
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{
            paddingHorizontal: theme.layout.screenX,
            paddingVertical: theme.layout.screenY,
            paddingBottom: theme.space.lg,
            gap: theme.space.xl,
          }}
        >
          <StockOverviewCard
            detail={detail}
            historyPending={historyPending}
            historyFetching={historyFetching}
            historyError={historyError}
            onRetryHistory={refetchHistory}
          />
          <StockInfoCard detail={detail} />

          <TradeButtons
            detail={detail}
            onTradeSuccess={() => {
              queryClient.invalidateQueries({ queryKey: walletKeys.all });
            }}
          />
        </ScrollView>
      ) : (
        <View style={[styles.empty, { gap: theme.space.lg }]}>
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
            {isPending ? '상품을 불러오는 중이에요.' : '상품을 불러오지 못했어요.'}
          </Text>
          {isError ? (
            <Button label="다시 시도" variant="outline" onPress={() => refetch()} />
          ) : null}
        </View>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
