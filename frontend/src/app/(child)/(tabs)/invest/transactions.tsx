import { router } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import {
  RealizedProfitCard,
  InvestmentQueryError,
  TransactionCard,
  useInvestmentTransactions,
} from '@/features/investment';
import { useTheme } from '@/shared/theme';
import { Header, Screen, usePullToRefresh } from '@/shared/ui';

/**
 * 투자 거래 내역. 매수/매도한 기록을 최신순으로 보여준다 (INV-T-05).
 */
export default function InvestTransactionsScreen() {
  const theme = useTheme();
  const refreshControl = usePullToRefresh();
  const transactions = useInvestmentTransactions();

  return (
    <Screen
      header={
        <Header
          title="투자 거래 내역"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/invest/index'))}
        />
      }
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
    >
      {transactions.isPending || transactions.isError ? (
        <View style={[styles.center, { gap: theme.space.lg }]}>
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
            {transactions.isPending ? '거래 내역을 불러오는 중이에요.' : '불러오지 못했어요.'}
          </Text>
          {transactions.isError ? (
            <InvestmentQueryError
              error={transactions.error}
              fetching={transactions.isFetching}
              onRetry={() => transactions.refetch()}
            />
          ) : null}
        </View>
      ) : transactions.data.length === 0 ? (
        <View style={styles.center}>
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
            아직 사고판 기록이 없어요.
          </Text>
        </View>
      ) : (
        <ScrollView
          refreshControl={refreshControl}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{
            paddingHorizontal: theme.layout.screenX,
            paddingVertical: theme.layout.screenY,
            paddingBottom: theme.space.lg,
            gap: theme.space.md,
          }}
        >
          <RealizedProfitCard transactions={transactions.data} />
          {transactions.data.map((item) => (
            <TransactionCard key={item.id} item={item} />
          ))}
        </ScrollView>
      )}
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
