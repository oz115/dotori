import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ScrollView, StyleSheet, Text, View, type StyleProp, type TextStyle } from 'react-native';

import {
  InvestmentSummaryCards,
  NewsCarousel,
  NewsDetailModal,
  SectionTitle,
  StockCard,
  summarizeInvestment,
  useInvestmentHoldings,
  useInvestmentNews,
  useInvestmentProducts,
  type InvestmentNews,
} from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Header } from '@/shared/ui';

// Figma 아이콘 시트에서 추출한 섹션 아이콘
const MEGAPHONE = require('@/assets/images/invest/megaphone.png');
const CLIPBOARD = require('@/assets/images/invest/clipboard.png');

/**
 * 자녀 투자 탭. 투자 요약(보유 목록에서 계산)·오늘의 뉴스·주식 리스트를 보여준다.
 * 탭 화면은 전환해도 언마운트되지 않아서, 돌아올 때마다 가격·보유를 다시 받아온다.
 */
export default function InvestScreen() {
  const theme = useTheme();
  const [selectedNews, setSelectedNews] = useState<InvestmentNews | null>(null);

  const products = useInvestmentProducts();
  const news = useInvestmentNews();
  const holdings = useInvestmentHoldings();

  const refetchProducts = products.refetch;
  const refetchHoldings = holdings.refetch;
  useFocusEffect(
    useCallback(() => {
      refetchProducts();
      refetchHoldings();
    }, [refetchProducts, refetchHoldings]),
  );

  const mutedText: StyleProp<TextStyle> = [
    theme.typography.bodySm,
    { color: theme.color.text.muted },
  ];

  return (
    <View style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}>
      <Header
        title="투자 상품 상세 보기"
        onBack={() => (router.canGoBack() ? router.back() : router.replace('/'))}
        action={{ label: '거래 내역', onPress: () => router.push('/invest/transactions') }}
      />

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingVertical: theme.layout.screenY,
          gap: theme.space['2xl'],
        }}
      >
        <InvestmentSummaryCards summary={summarizeInvestment(holdings.data ?? [])} />

        <View style={{ gap: theme.space.lg }}>
          <SectionTitle icon={MEGAPHONE} title="오늘의 뉴스" />
          {news.isPending ? (
            <Text style={mutedText}>오늘의 뉴스를 불러오는 중이에요.</Text>
          ) : news.isError ? (
            <Text style={mutedText}>뉴스를 불러오지 못했어요.</Text>
          ) : news.data.length === 0 ? (
            <Text style={mutedText}>오늘의 뉴스가 아직 없어요.</Text>
          ) : (
            <NewsCarousel items={news.data} onPressItem={setSelectedNews} />
          )}
        </View>

        <View style={{ gap: theme.space.lg }}>
          <SectionTitle
            icon={CLIPBOARD}
            title="주식 리스트"
            action={{
              label: '보유한 주식 보기',
              onPress: () => router.push('/invest/holdings'),
            }}
          />
          {products.isPending ? (
            <Text style={mutedText}>주식을 불러오는 중이에요.</Text>
          ) : products.isError ? (
            <Text style={mutedText}>주식을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</Text>
          ) : (
            <View style={{ gap: theme.space.md }}>
              {products.data.map((stock) => (
                <StockCard
                  key={stock.id}
                  stock={stock}
                  onPress={() =>
                    router.push({
                      pathname: '/invest/[stockId]',
                      params: { stockId: String(stock.id) },
                    })
                  }
                />
              ))}
            </View>
          )}
        </View>
      </ScrollView>

      <NewsDetailModal news={selectedNews} onClose={() => setSelectedNews(null)} />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
});
