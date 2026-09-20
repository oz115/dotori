import { useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';

import { summarizeInvestment } from '@/features/investment/lib/mappers';
import {
  recentTransactions,
  summarizeRealizedProfit,
} from '@/features/investment/lib/transactions';
import { useInvestmentHoldings } from '@/features/investment/model/useInvestmentHoldings';
import { useInvestmentTransactions } from '@/features/investment/model/useInvestmentTransactions';
import {
  ChildInvestmentSummary,
  type RecentSalesProfit,
} from '@/features/investment/ui/ChildInvestmentSummary';
import { HoldingsListCard } from '@/features/investment/ui/HoldingsListCard';
import { InvestmentMenuCard } from '@/features/investment/ui/InvestmentMenuCard';
import { InvestmentQueryError } from '@/features/investment/ui/InvestmentQueryError';
import { SectionTitle } from '@/features/investment/ui/SectionTitle';
import { useTheme } from '@/shared/theme';
import { FilterChips, Header, Screen } from '@/shared/ui';

// Figma 아이콘 시트에서 추출한 섹션 아이콘 (자녀 투자 화면과 같은 세트)
const CANDLES = require('@/assets/images/invest/candles.png');
const CLIPBOARD = require('@/assets/images/invest/clipboard.png');
const MEGAPHONE = require('@/assets/images/invest/megaphone.png');
const SPROUT = require('@/assets/images/invest/sprout.png');
// 지난 뉴스 해설 카드의 일러스트 — 책 읽는 부엉이 (부모 투자 전용)
const OWL = require('@/assets/images/invest/parent-investment.png');

/** 부모가 "일주일 동안 어떻게 사고팔았는지" 볼 기간(일). 주기 기준이 정해지면 서버 값으로 바꾼다 */
export const PARENT_RECENT_DAYS = 7;

/** 요약 띠에 미리 보여줄 보유 종목 수. 나머지는 "전체 보기"로 넘긴다 */
const HOLDINGS_PREVIEW = 3;

/** 가족 슬라이스 값이라 라우트가 넘긴다 — features 는 서로 직접 import 하지 않는다 */
export type ParentInvestmentChild = { childId: number; name: string };

type Props = {
  childList: ParentInvestmentChild[];
  /** 종목 상세로. childId 는 그 자녀의 보유 여부를 상세에서 보여주려고 함께 넘긴다 */
  onPressStock: (productId: number, childId: number | undefined) => void;
  onPressHoldings: (childId: number) => void;
  onPressTransactions: (childId: number) => void;
  /** 뉴스·시세는 가족 단위라 자녀와 무관하지만, 시세에서 상세로 넘어갈 때 쓰도록 자녀를 함께 넘긴다 */
  onPressMarket: (childId: number | undefined) => void;
  onPressNews: () => void;
  onPressNewsReview: () => void;
};

/**
 * 부모 투자 탭의 첫 화면. 위쪽 연회색 띠에 "내 아이의 투자 요약"을 한 덩어리로 묶고,
 * 아래에는 색이 다른 카드 4장(거래 내역·오늘의 뉴스·주식 시세·지난 뉴스 해설)으로 각 페이지에 들어간다.
 * 한 화면에 전부 담으면 길어서 보기 어려워, 요약만 남기고 나머지는 페이지로 나눴다.
 *
 * 부모는 사고팔 수 없다 — 백엔드도 매수·매도를 자녀만 허용하므로 사기/팔기 버튼은 어디에도 없다.
 * 자녀가 둘 이상일 때만 고르는 칩이 뜨고, 항상 한 명이 골라져 있다(기본은 첫째).
 * 요약·거래 조회가 모두 아이 단위라 여러 명을 한 화면에 쌓지 않고 한 명씩 본다.
 */
export function ParentInvestmentHome({
  childList,
  onPressStock,
  onPressHoldings,
  onPressTransactions,
  onPressMarket,
  onPressNews,
  onPressNewsReview,
}: Props) {
  const theme = useTheme();
  const [selected, setSelected] = useState<string | null>(null);

  const child = childList.find((c) => String(c.childId) === selected) ?? childList[0];
  const childId = child?.childId;

  const holdings = useInvestmentHoldings(childId, { enabled: childId != null });
  const transactions = useInvestmentTransactions(childId, { enabled: childId != null });

  const [refreshing, setRefreshing] = useState(false);
  const { refetch: refetchHoldings } = holdings;
  const { refetch: refetchTransactions } = transactions;
  const refresh = useCallback(async () => {
    // 자녀 목록이 오기 전에는 보유·거래 요청을 보내지 않는다 (두 훅이 꺼져 있다)
    if (childId == null) return;
    setRefreshing(true);
    try {
      await Promise.all([refetchHoldings(), refetchTransactions()]);
    } finally {
      setRefreshing(false);
    }
  }, [refetchHoldings, refetchTransactions, childId]);
  // 탭 화면은 전환해도 언마운트되지 않아서, 돌아올 때마다 다시 받아온다
  useFocusEffect(
    useCallback(() => {
      void refresh();
    }, [refresh]),
  );

  const filters = useMemo(
    () => childList.map((c) => ({ key: String(c.childId), label: c.name })),
    [childList],
  );

  // 날짜 경계는 렌더 시점마다 새로 계산한다 — 화면을 켜 둔 채 자정이 지나도 기간이 밀린다
  const recent = transactions.data
    ? recentTransactions(transactions.data, PARENT_RECENT_DAYS)
    : undefined;
  const sales: RecentSalesProfit = transactions.isError
    ? { status: 'error' }
    : recent
      ? { status: 'ready', ...summarizeRealizedProfit(recent) }
      : { status: 'pending' };

  const muted = [theme.typography.bodySm, { color: theme.color.text.sub }];

  return (
    <Screen
      header={<Header title="투자" />}
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
    >
      <ScrollView
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />}
        contentContainerStyle={{ paddingBottom: theme.space['4xl'] }}
      >
        {/* 내 아이의 투자 요약 — 홈의 활동 보드처럼 연회색 띠 하나로 묶는다 */}
        <View
          style={{
            backgroundColor: theme.color.background.surfaceSunken,
            paddingHorizontal: theme.layout.screenX,
            paddingTop: theme.space.xl,
            paddingBottom: theme.space['2xl'],
            gap: theme.space.lg,
          }}
        >
          {childList.length > 1 ? (
            <FilterChips
              options={filters}
              selected={child ? String(child.childId) : null}
              onSelect={setSelected}
              showAll={false}
            />
          ) : null}

          {child == null ? (
            <Text style={muted}>
              아직 연결된 아이가 없어요.{'\n'}아이를 초대하면 투자 현황을 볼 수 있어요.
            </Text>
          ) : (
            <>
              <SectionTitle icon={CANDLES} title={`${child.name}의 투자`} />
              {holdings.isPending ? (
                <Text style={muted}>투자 현황을 불러오는 중이에요.</Text>
              ) : holdings.isError ? (
                <InvestmentQueryError
                  error={holdings.error}
                  fetching={holdings.isFetching}
                  onRetry={() => holdings.refetch()}
                />
              ) : (
                <>
                  <ChildInvestmentSummary
                    summary={summarizeInvestment(holdings.data)}
                    sales={sales}
                    days={PARENT_RECENT_DAYS}
                  />
                  {holdings.data.length > 0 ? (
                    <View style={{ gap: theme.space.md }}>
                      <SectionTitle
                        title={`보유 주식 ${holdings.data.length}개`}
                        action={
                          holdings.data.length > HOLDINGS_PREVIEW
                            ? { label: '전체 보기', onPress: () => onPressHoldings(child.childId) }
                            : undefined
                        }
                      />
                      <HoldingsListCard
                        holdings={holdings.data.slice(0, HOLDINGS_PREVIEW)}
                        onPressHolding={(holding) => onPressStock(holding.id, child.childId)}
                      />
                    </View>
                  ) : null}
                </>
              )}
            </>
          )}
        </View>

        {/* 페이지로 들어가는 색 카드 — 섹션마다 색을 달리해 한눈에 구분한다 */}
        <View
          style={{
            paddingHorizontal: theme.layout.screenX,
            paddingTop: theme.space['2xl'],
            gap: theme.space.md,
          }}
        >
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            투자 둘러보기
          </Text>
          <View style={[styles.row, { gap: theme.space.md }]}>
            <InvestmentMenuCard
              title="거래 내역"
              subtitle={
                recent
                  ? `최근 ${PARENT_RECENT_DAYS}일 ${recent.length}건`
                  : '아이가 사고판 기록이에요'
              }
              icon={CLIPBOARD}
              tint={theme.color.status.warning.subtle}
              onPress={() => childId != null && onPressTransactions(childId)}
            />
            <InvestmentMenuCard
              title="오늘의 뉴스"
              subtitle="아이가 읽는 뉴스예요"
              icon={MEGAPHONE}
              tint={theme.color.status.info.subtle}
              onPress={onPressNews}
            />
          </View>
          <View style={[styles.row, { gap: theme.space.md }]}>
            <InvestmentMenuCard
              title="주식 시세"
              subtitle="오늘 가격과 어제 대비"
              icon={SPROUT}
              tint={theme.color.brand.subtle}
              onPress={() => onPressMarket(childId)}
            />
            <InvestmentMenuCard
              title="지난 뉴스 해설"
              subtitle="어제 뉴스의 결과예요"
              icon={OWL}
              tint={theme.color.background.surfaceSunken}
              onPress={onPressNewsReview}
            />
          </View>
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row' },
});
