import { router } from 'expo-router';
import { ScrollView, StyleSheet, View } from 'react-native';

import {
  InvestmentSummaryCards,
  mockInvestmentNews,
  mockInvestmentSummary,
  mockStocks,
  NewsCarousel,
  SectionTitle,
  StockCard,
} from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { BottomNav, Header, type NavIconName } from '@/shared/ui';

const NAV_ITEMS: { key: NavIconName; label: string }[] = [
  { key: 'task', label: '할 일' },
  { key: 'store', label: '상점' },
  { key: 'home', label: '홈' },
  { key: 'save', label: '저축' },
  { key: 'invest', label: '투자' },
];

/**
 * 자녀 투자 탭 (Figma: 투자 상품 상세 보기). 투자 요약·오늘의 뉴스·주식 리스트를
 * 보여준다. 백엔드 투자 API 연동 전이라 목업 데이터로 배치만 잡아둔 상태.
 */
export default function InvestScreen() {
  const theme = useTheme();

  const onSelectTab = (key: NavIconName) => {
    if (key === 'invest') {
      return;
    }
    if (key === 'home') {
      router.replace('/');
      return;
    }
    // TODO: 할 일·상점·저축 탭은 해당 화면 태스크에서 라우트가 생기면 연결한다
  };

  return (
    <View style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}>
      <Header
        title="투자 상품 상세 보기"
        onBack={() => (router.canGoBack() ? router.back() : router.replace('/'))}
      />

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingVertical: theme.layout.screenY,
          gap: theme.space['2xl'],
        }}
      >
        <InvestmentSummaryCards summary={mockInvestmentSummary} />

        <View style={{ gap: theme.space.lg }}>
          <SectionTitle emoji="📢" title="오늘의 뉴스" />
          <NewsCarousel items={mockInvestmentNews} />
        </View>

        <View style={{ gap: theme.space.lg }}>
          <SectionTitle
            emoji="📋"
            title="주식 리스트"
            action={{
              label: '보유한 주식 보기',
              // TODO: 보유 주식 화면 태스크에서 라우트가 생기면 연결한다
              onPress: () => {},
            }}
          />
          <View style={{ gap: theme.space.md }}>
            {mockStocks.map((stock) => (
              <StockCard
                key={stock.id}
                stock={stock}
                // TODO: 종목 상세 화면 태스크에서 라우트가 생기면 연결한다
                onPress={() => {}}
              />
            ))}
          </View>
        </View>
      </ScrollView>

      <BottomNav items={NAV_ITEMS} activeKey="invest" onSelect={onSelectTab} />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
});
