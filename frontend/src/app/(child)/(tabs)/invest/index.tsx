import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { RefreshControl, ScrollView, Text, View } from 'react-native';

import {
  InvestmentSummaryCards,
  InvestmentQueryError,
  NewsCarousel,
  SectionTitle,
  StockCard,
  summarizeInvestment,
  useInvestmentHoldings,
  useInvestmentNews,
  useInvestmentProducts,
} from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Header, HeaderChipButton, Screen } from '@/shared/ui';

// Figma 아이콘 시트에서 추출한 섹션 아이콘
const MEGAPHONE = require('@/assets/images/invest/megaphone.png');
const CLIPBOARD = require('@/assets/images/invest/clipboard.png');

/**
 * 자녀 투자 탭. 내 주식 요약(보유 목록에서 계산한 보유 가치·순이익)·오늘의 뉴스·주식 리스트를
 * 보여준다. 지갑 잔액(포인트)은 여기서 보여주지 않는다 — 홈·지갑 화면의 몫이다.
 * 탭 화면은 전환해도 언마운트되지 않아서, 돌아올 때마다 가격·뉴스·보유를 다시 받아온다.
 */
export default function InvestScreen() {
  const theme = useTheme();
  const products = useInvestmentProducts();
  const news = useInvestmentNews();
  const holdings = useInvestmentHoldings();
  const [refreshing, setRefreshing] = useState(false);
  // 뉴스 캐러셀에 손이 닿아 있는 동안은 당겨서 새로고침을 꺼 둔다 —
  // 가로 스와이프에 섞인 세로 움직임이 새로고침을 잘못 당기는 것을 막는다.
  const [newsTouching, setNewsTouching] = useState(false);
  const { refetch: refetchProducts } = products;
  const { refetch: refetchNews } = news;
  const { refetch: refetchHoldings } = holdings;
  const refresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await Promise.all([refetchProducts(), refetchNews(), refetchHoldings()]);
    } finally {
      setRefreshing(false);
    }
  }, [refetchProducts, refetchNews, refetchHoldings]);
  useFocusEffect(
    useCallback(() => {
      void refresh();
    }, [refresh]),
  );
  const muted = [theme.typography.bodySm, { color: theme.color.text.sub }];
  return (
    <Screen
      header={
        <Header
          title="투자"
          right={
            <HeaderChipButton
              label="거래 내역"
              onPress={() => router.push('/invest/transactions')}
            />
          }
        />
      }
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
      style={{ paddingTop: theme.space.sm }}
    >
      <ScrollView
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={refresh} enabled={!newsTouching} />
        }
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingBottom: theme.space['4xl'],
          gap: theme.space['2xl'],
        }}
      >
        {holdings.isPending ? (
          <Text style={muted}>내 주식을 불러오는 중이에요.</Text>
        ) : holdings.isError ? (
          <InvestmentQueryError
            error={holdings.error}
            fetching={holdings.isFetching}
            onRetry={() => holdings.refetch()}
          />
        ) : (
          <InvestmentSummaryCards summary={summarizeInvestment(holdings.data)} />
        )}

        <View style={{ gap: theme.space.lg }}>
          <SectionTitle icon={MEGAPHONE} title="오늘의 뉴스" />
          {/* 뉴스마다 반복하던 안내를 섹션에서 한 번만 말한다 */}
          <Text style={muted}>어떤 회사에 영향을 줄지 생각해 보세요!</Text>
          {news.isPending ? (
            <Text style={muted}>오늘의 뉴스를 불러오는 중이에요.</Text>
          ) : news.isError ? (
            <InvestmentQueryError
              error={news.error}
              fetching={news.isFetching}
              onRetry={() => news.refetch()}
            />
          ) : news.data.length === 0 ? (
            <Text style={muted}>오늘의 뉴스가 아직 없어요.</Text>
          ) : (
            <NewsCarousel
              items={news.data}
              onTouchingChange={setNewsTouching}
              onPressItem={(item) =>
                router.push({
                  pathname: '/invest/news/[newsId]',
                  params: { newsId: String(item.id) },
                })
              }
            />
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
          <Text style={muted}>현재 가격과 어제보다 오른 만큼(내린 만큼)을 함께 보여줘요.</Text>
          {products.isPending ? (
            <Text style={muted}>주식을 불러오는 중이에요.</Text>
          ) : products.isError ? (
            <InvestmentQueryError
              error={products.error}
              fetching={products.isFetching}
              onRetry={() => products.refetch()}
            />
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
    </Screen>
  );
}
