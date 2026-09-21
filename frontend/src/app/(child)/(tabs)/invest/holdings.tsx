import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import {
  HoldingCard,
  InvestmentQueryError,
  HoldingsSummaryCard,
  TipBanner,
  TradeSelectionBar,
  useInvestmentHoldings,
} from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Header, Screen, usePullToRefresh } from '@/shared/ui';

/**
 * 보유 투자 상품. 합계 요약·보유 종목 목록·교육 팁의 배치만 담당하고,
 * 데이터는 보유 조회 API(useInvestmentHoldings) 하나에서 나온다.
 * 카드를 꾹 누르면 선택 모드가 되어 여러 종목을 한 번에 팔 수 있다.
 */
export default function InvestHoldingsScreen() {
  const theme = useTheme();
  const refreshControl = usePullToRefresh();
  const holdings = useInvestmentHoldings();

  // 꾹 눌러 켜는 일괄 팔기 선택 모드 — 하나라도 골라져 있으면 켜진 상태다
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const selectionMode = selectedIds.length > 0;
  const toggleSelect = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((selected) => selected !== id) : [...prev, id],
    );
  };

  return (
    <Screen
      header={
        <Header
          title="보유 투자 상품 보기"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/invest/index'))}
        />
      }
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
    >
      {holdings.isPending || holdings.isError ? (
        <View style={[styles.center, { gap: theme.space.lg }]}>
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
            {holdings.isPending ? '보유한 주식을 불러오는 중이에요.' : '불러오지 못했어요.'}
          </Text>
          {holdings.isError ? (
            <InvestmentQueryError
              error={holdings.error}
              fetching={holdings.isFetching}
              onRetry={() => holdings.refetch()}
            />
          ) : null}
        </View>
      ) : (
        <ScrollView
          refreshControl={refreshControl}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{
            paddingHorizontal: theme.layout.screenX,
            paddingVertical: theme.layout.screenY,
            paddingBottom: theme.space.lg,
            gap: theme.space['2xl'],
          }}
        >
          <HoldingsSummaryCard holdings={holdings.data} />

          <View style={{ gap: theme.space.lg }}>
            <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>
              보유한 투자 상품
            </Text>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
              카드를 꾹 누르면 여러 개를 골라 한 번에 팔 수 있어요.
            </Text>
            {holdings.data.length === 0 ? (
              <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
                아직 산 주식이 없어요. 주식 리스트에서 마음에 드는 회사를 골라 보세요!
              </Text>
            ) : (
              <View style={{ gap: theme.space.md }}>
                {holdings.data.map((holding) => (
                  <HoldingCard
                    key={holding.id}
                    holding={holding}
                    selectionMode={selectionMode}
                    selected={selectedIds.includes(holding.id)}
                    onLongPress={() => toggleSelect(holding.id)}
                    onPress={() =>
                      selectionMode
                        ? toggleSelect(holding.id)
                        : router.push({
                            pathname: '/invest/[stockId]',
                            params: { stockId: String(holding.id) },
                          })
                    }
                  />
                ))}
              </View>
            )}
          </View>

          <TipBanner message={'여러 상품을 나누어 가지면\n더 안정적으로 배울 수 있어요.'} />
        </ScrollView>
      )}

      {selectionMode ? (
        <TradeSelectionBar
          mode="sell"
          items={(holdings.data ?? [])
            .filter((holding) => selectedIds.includes(holding.id))
            .map((holding) => ({
              id: holding.id,
              name: holding.name,
              price: holding.currentPrice,
            }))}
          onClose={() => setSelectedIds([])}
        />
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
