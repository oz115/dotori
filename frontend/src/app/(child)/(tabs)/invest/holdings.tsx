import { router } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { HoldingCard, HoldingsSummaryCard, mockHoldings, TipBanner } from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Header } from '@/shared/ui';

/**
 * 보유 투자 상품 (Figma: 보유 투자 상품 보기). 합계 요약·보유 종목 목록·교육 팁의
 * 배치만 담당한다. 백엔드 투자 API 연동 전이라 목업 데이터를 보여 준다.
 */
export default function InvestHoldingsScreen() {
  const theme = useTheme();

  return (
    <View style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}>
      <Header
        title="보유 투자 상품 보기"
        onBack={() => (router.canGoBack() ? router.back() : router.replace('/invest/index'))}
      />

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingVertical: theme.layout.screenY,
          gap: theme.space['2xl'],
        }}
      >
        <HoldingsSummaryCard holdings={mockHoldings} />

        <View style={{ gap: theme.space.lg }}>
          <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>
            보유한 투자 상품
          </Text>
          <View style={{ gap: theme.space.md }}>
            {mockHoldings.map((detail) => (
              <HoldingCard
                key={detail.id}
                detail={detail}
                onPress={() =>
                  router.push({
                    pathname: '/invest/[stockId]',
                    params: { stockId: String(detail.id) },
                  })
                }
              />
            ))}
          </View>
        </View>

        <TipBanner message={'여러 상품을 나누어 가지면\n더 안정적으로 배울 수 있어요.'} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
});
