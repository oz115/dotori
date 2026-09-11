import { router, useLocalSearchParams } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { findMockStockDetail, StockInfoCard, StockOverviewCard } from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Button, Header } from '@/shared/ui';
import { MainTabBar } from '@/widgets/main-tab-bar';

/**
 * 투자 상품 상세 (Figma: 투자 상품 상세 보기). 개요 카드·상품 정보·사기/팔기 버튼의
 * 배치만 담당한다. 백엔드 투자 API 연동 전이라 목업 데이터를 보여 준다.
 */
export default function InvestDetailScreen() {
  const theme = useTheme();
  const { stockId } = useLocalSearchParams<{ stockId: string }>();
  const detail = findMockStockDetail(Number(stockId));

  return (
    <View style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}>
      <Header
        title="투자 상품 상세 보기"
        onBack={() => (router.canGoBack() ? router.back() : router.replace('/invest'))}
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

          {/* TODO: 매수·매도 태스크에서 주문 플로우가 생기면 연결한다 */}
          <View style={[styles.tradeRow, { gap: theme.space.md }]}>
            <Button label="🌰 사기" variant="primary" style={styles.flex} onPress={() => {}} />
            <Button label="🌰 팔기" variant="outline" style={styles.flex} onPress={() => {}} />
          </View>
        </ScrollView>
      ) : (
        <View style={styles.empty}>
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
            상품을 찾을 수 없어요.
          </Text>
        </View>
      )}

      <MainTabBar active="invest" />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  tradeRow: {
    flexDirection: 'row',
  },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
