import { router, useLocalSearchParams } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { useFamilyId, useFamilyMembers } from '@/features/family';
import {
  formatAcorns,
  StockInfoCard,
  StockOverviewCard,
  useStockDetail,
} from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Button, ContentCard, Header, Screen } from '@/shared/ui';

/**
 * 부모용 투자 상품 상세. 아이가 보는 화면(차트·상품 정보)과 같은 내용을 읽기 전용으로 보여준다.
 * 부모는 사고팔 수 없으므로 사기/팔기 버튼이 없고, 대신 이 종목을 그 자녀가 들고 있는지 알려준다.
 * 자녀는 종목 목록·보유 카드에서 넘어온 childId 로 정한다.
 */
export default function ParentInvestmentDetailScreen() {
  const theme = useTheme();
  const { stockId, childId: childIdParam } = useLocalSearchParams<{
    stockId: string;
    childId?: string;
  }>();
  const childId = childIdParam != null ? Number(childIdParam) : undefined;

  const { data: familyId } = useFamilyId();
  const { data: members } = useFamilyMembers(familyId ?? undefined);
  const childName = members?.find((member) => member.userId === childId)?.name;

  const {
    detail,
    isPending,
    isError,
    refetch,
    historyPending,
    historyError,
    historyFetching,
    refetchHistory,
  } = useStockDetail(Number(stockId), childId);

  return (
    <Screen
      header={
        <Header
          title="투자 상품 상세 보기"
          onBack={() =>
            router.canGoBack() ? router.back() : router.replace('/(parent)/(tabs)/investment')
          }
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
            paddingTop: theme.layout.screenY,
            // 탭바가 스크롤 영역 아래를 덮으므로 마지막 항목이 가려지지 않게 하단 여백을 넉넉히 둔다
            paddingBottom: theme.space['4xl'],
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

          {childName != null ? (
            <ContentCard style={{ padding: theme.space.lg, gap: theme.space.sm }}>
              <View style={styles.row}>
                <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
                  {childName}의 보유 현황
                </Text>
                <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                  {detail.holding ? '보유 중' : '보유하지 않음'}
                </Text>
              </View>
              {detail.holding && detail.buyPrice != null ? (
                <View style={styles.row}>
                  <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
                    산 가격
                  </Text>
                  <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                    {formatAcorns(detail.buyPrice)}
                  </Text>
                </View>
              ) : null}
            </ContentCard>
          ) : null}
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
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center' },
});
