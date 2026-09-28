import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { useInvestmentTransactions } from '@/features/investment/model/useInvestmentTransactions';
import { InvestmentQueryError } from '@/features/investment/ui/InvestmentQueryError';
import { TransactionCard } from '@/features/investment/ui/TransactionCard';
import { isoWithinRange } from '@/shared/lib/dateRange';
import { useTheme } from '@/shared/theme';
import {
  Header,
  PeriodFilterControl,
  Screen,
  defaultPeriodFilter,
  periodDateRange,
  usePullToRefresh,
} from '@/shared/ui';

type Props = {
  /** 없으면 부모 본인 거래를 묻게 되므로 조회하지 않는다 */
  childId: number | undefined;
  childName: string | undefined;
  /** 직접설정 달력의 하한 (가입일). 라우트가 프로필에서 꺼내 넘긴다 */
  minPickableDate?: Date;
  onBack: () => void;
};

/**
 * 자녀의 투자 거래 내역 (부모). 처음에는 **전체** 거래를 최신순으로 보여주고,
 * 기간 조회는 실계좌 거래내역과 같은 달력 방식(PeriodFilterControl)이다 —
 * 앱의 모든 내역 화면이 같은 방법으로 기간을 고르게 통일했다.
 */
export function ParentInvestmentTransactions({
  childId,
  childName,
  minPickableDate,
  onBack,
}: Props) {
  const theme = useTheme();
  const refreshControl = usePullToRefresh();
  const [period, setPeriod] = useState(defaultPeriodFilter());
  const transactions = useInvestmentTransactions(childId, { enabled: childId != null });
  const muted = [theme.typography.body, { color: theme.color.text.muted }];

  const range = periodDateRange(period);
  const items = transactions.data;
  const visible = items
    ? range
      ? items.filter((item) => isoWithinRange(item.createdAt, range))
      : items
    : undefined;

  return (
    <Screen
      header={
        <Header title={childName ? `${childName}의 거래 내역` : '투자 거래 내역'} onBack={onBack} />
      }
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
    >
      {childId == null ? (
        <View style={styles.center}>
          <Text style={muted}>어느 아이의 거래인지 알 수 없어요.</Text>
        </View>
      ) : (
        <ScrollView
          refreshControl={refreshControl}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{
            paddingHorizontal: theme.layout.screenX,
            paddingTop: theme.layout.screenY,
            // 탭바가 스크롤 영역 아래를 덮으므로 마지막 항목이 가려지지 않게 하단 여백을 넉넉히 둔다
            paddingBottom: theme.space['4xl'],
            gap: theme.space.md,
          }}
        >
          <PeriodFilterControl
            value={period}
            onChange={setPeriod}
            minPickableDate={minPickableDate}
          />

          {visible == null ? (
            transactions.isError ? (
              <InvestmentQueryError
                error={transactions.error}
                fetching={transactions.isFetching}
                onRetry={() => transactions.refetch()}
              />
            ) : (
              <Text style={muted}>거래 내역을 불러오는 중이에요.</Text>
            )
          ) : visible.length === 0 ? (
            <Text style={muted}>
              {range ? '이 기간에는 사고판 주식이 없어요.' : '아직 사고판 기록이 없어요.'}
            </Text>
          ) : (
            <>
              <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
                {range ? '이 기간' : '전체'} {visible.length}건
              </Text>
              {visible.map((item) => (
                <TransactionCard key={item.id} item={item} />
              ))}
            </>
          )}
        </ScrollView>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
});
