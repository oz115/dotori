import { router, useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, BackHandler, RefreshControl, ScrollView, Text, View } from 'react-native';

import {
  InvestmentSummaryCards,
  InvestmentQueryError,
  NewsCarousel,
  SectionTitle,
  StockCard,
  TradeSelectionBar,
  summarizeInvestment,
  useInvestmentHoldings,
  useInvestmentNews,
  useInvestmentProducts,
  type StockSummary,
} from '@/features/investment';
import { fontFamily, useTheme } from '@/shared/theme';
import { Checkbox, Header, HeaderChipButton, Screen } from '@/shared/ui';

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

  // 카드를 꾹 누르면 여러 종목을 골라 한 번에 사는 선택 모드. 하나라도 골라져 있으면 켜진 상태다.
  // "보유한 것만" 을 켜면 목록이 내 주식만 남고, 꾹 눌러 고르는 동작도 사기 → 팔기로 바뀐다.
  // 한 목록에서 사기와 팔기가 섞이면 무엇을 누르는 중인지 알 수 없어, 체크박스로 갈라 둔다.
  const [onlyHeld, setOnlyHeld] = useState(false);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const selectionMode = selectedIds.length > 0;
  const heldIds = useMemo(
    () => new Set((holdings.data ?? []).map((holding) => holding.id)),
    [holdings.data],
  );
  const toggleSelect = (stock: StockSummary) => {
    // 종목당 1주 규칙 — 사는 중이라면 이미 보유한 종목은 고를 수 없다
    if (!onlyHeld && heldIds.has(stock.id)) {
      Alert.alert('이미 가지고 있는 주식이에요', '한 회사는 1주만 살 수 있어요.');
      return;
    }
    setSelectedIds((prev) =>
      prev.includes(stock.id) ? prev.filter((id) => id !== stock.id) : [...prev, stock.id],
    );
  };
  // 하단 "취소" 버튼만으로는 빠져나갈 길이 하나뿐이다. 안드로이드에서는 뒤로가기로 무언가를
  // 그만두는 것이 기본 동작이라, 선택 모드일 때는 뒤로가기가 화면을 떠나는 대신 선택을 지운다 (QA).
  useEffect(() => {
    if (!selectionMode) return;
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      setSelectedIds([]);
      return true;
    });
    return () => subscription.remove();
  }, [selectionMode]);

  // 보유 여부로 정렬하지는 않는다 — 목록 순서가 바뀌면 "어디 있던 그 카드" 를 다시 찾아야 한다.
  // 초록 테두리로만 구분하고, 내 주식만 보고 싶으면 체크박스로 걸러 낸다.
  const visibleStocks = useMemo(
    () =>
      onlyHeld
        ? (products.data ?? []).filter((stock) => heldIds.has(stock.id))
        : (products.data ?? []),
    [products.data, onlyHeld, heldIds],
  );

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

        <View style={{ gap: theme.space.xl }}>
          {/* 제목과 안내문은 한 덩어리로 읽히게 붙이고(8), 뉴스 카드와는 넉넉히 띄운다(20).
              전에는 셋이 모두 16 이라 제목·안내문·카드가 서로 어디에 딸린 것인지 안 보였다 (QA) */}
          <View style={{ gap: theme.space.sm }}>
            <SectionTitle icon={MEGAPHONE} title="오늘의 뉴스" />
            {/* 뉴스마다 반복하던 안내를 섹션에서 한 번만 말한다 */}
            <Text style={muted}>어떤 회사에 영향을 줄지 생각해 보세요!</Text>
          </View>
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
            right={
              <Checkbox
                shape="circle"
                checked={onlyHeld}
                onChange={(next) => {
                  // 사기/팔기가 섞이지 않도록 모드를 바꿀 때 고르던 것은 버린다
                  setSelectedIds([]);
                  setOnlyHeld(next);
                }}
                label="보유한 주식 보기"
              />
            }
          />
          {/* 문장을 한 덩어리로 붙이면 안 읽힌다 — 줄을 나누고 조작 안내는 굵게 강조 */}
          <View style={{ gap: theme.space.xs }}>
            <Text style={muted}>현재 가격과 어제보다 오른 만큼(내린 만큼)을 함께 보여줘요.</Text>
            <Text style={[...muted, { fontFamily: fontFamily.bold }]}>
              {onlyHeld
                ? '카드를 꾹 누르면 여러 개를 골라 한 번에 팔 수 있어요.'
                : '카드를 꾹 누르면 여러 개를 골라 한 번에 살 수 있어요.'}
            </Text>
          </View>
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
              {visibleStocks.length === 0 ? (
                <Text style={muted}>아직 산 주식이 없어요. 마음에 드는 회사를 골라 보세요!</Text>
              ) : (
                visibleStocks.map((stock) => {
                  const held = heldIds.has(stock.id);
                  return (
                    <StockCard
                      key={stock.id}
                      stock={stock}
                      held={held}
                      selectionMode={selectionMode && (onlyHeld || !held)}
                      selected={selectedIds.includes(stock.id)}
                      onLongPress={() => toggleSelect(stock)}
                      onPress={() =>
                        selectionMode
                          ? toggleSelect(stock)
                          : router.push({
                              pathname: '/invest/[stockId]',
                              params: { stockId: String(stock.id) },
                            })
                      }
                    />
                  );
                })
              )}
            </View>
          )}
        </View>
      </ScrollView>

      {selectionMode ? (
        <TradeSelectionBar
          mode={onlyHeld ? 'sell' : 'buy'}
          items={(products.data ?? [])
            .filter((stock) => selectedIds.includes(stock.id))
            .map((stock) => ({ id: stock.id, name: stock.name, price: stock.currentPrice }))}
          onClose={() => setSelectedIds([])}
        />
      ) : null}
    </Screen>
  );
}
