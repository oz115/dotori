import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import {
  hasTransactionsBefore,
  transactionPeriod,
  transactionsInPeriod,
} from '@/features/investment/lib/transactions';
import { useInvestmentTransactions } from '@/features/investment/model/useInvestmentTransactions';
import { InvestmentQueryError } from '@/features/investment/ui/InvestmentQueryError';
import { PeriodRangeBar } from '@/features/investment/ui/PeriodRangeBar';
import { TransactionCard } from '@/features/investment/ui/TransactionCard';
import { useTheme } from '@/shared/theme';
import { FilterChips, Header, Screen } from '@/shared/ui';

/** 기간 필터가 한 번에 보여주는 일수. 투자 주기 기준이 정해지면 서버 값으로 바꾼다 */
const PERIOD_DAYS = 7;

type Props = {
  /** 없으면 부모 본인 거래를 묻게 되므로 조회하지 않는다 */
  childId: number | undefined;
  childName: string | undefined;
  onBack: () => void;
};

/**
 * 자녀의 투자 거래 내역 (부모). 처음에는 **전체** 거래를 최신순으로 보여준다.
 * "7일" 칩을 누르면 기간 이동 막대(‹ 2026.09.15 ~ 2026.09.21 ›)가 나타나 7일씩 넘겨 볼 수 있다.
 * 오늘(KST)을 끝으로 하는 7일부터 시작하고, 거래가 있는 가장 이른 기간까지만 이전으로 갈 수 있다.
 */
export function ParentInvestmentTransactions({ childId, childName, onBack }: Props) {
  const theme = useTheme();
  const [byPeriod, setByPeriod] = useState(false);
  const [offset, setOffset] = useState(0);
  const transactions = useInvestmentTransactions(childId, { enabled: childId != null });
  const muted = [theme.typography.body, { color: theme.color.text.muted }];

  // 날짜 경계는 렌더 시점마다 새로 계산한다 — 화면을 켜 둔 채 자정이 지나도 기간이 밀린다
  const period = transactionPeriod(PERIOD_DAYS, offset);

  const items = transactions.data;
  const visible = items ? (byPeriod ? transactionsInPeriod(items, period) : items) : undefined;

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
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{
            paddingHorizontal: theme.layout.screenX,
            paddingTop: theme.layout.screenY,
            // 탭바가 스크롤 영역 아래를 덮으므로 마지막 항목이 가려지지 않게 하단 여백을 넉넉히 둔다
            paddingBottom: theme.space['4xl'],
            gap: theme.space.md,
          }}
        >
          <FilterChips
            options={[{ key: 'period', label: `${PERIOD_DAYS}일` }]}
            selected={byPeriod ? 'period' : null}
            onSelect={(key) => {
              setByPeriod(key === 'period');
              setOffset(0);
            }}
            allLabel="전체"
          />
          {byPeriod ? (
            <PeriodRangeBar
              label={period.label}
              canPrev={items ? hasTransactionsBefore(items, period) : false}
              canNext={offset > 0}
              onPrev={() => setOffset((current) => current + 1)}
              onNext={() => setOffset((current) => Math.max(current - 1, 0))}
            />
          ) : null}

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
              {byPeriod ? '이 기간에는 사고판 주식이 없어요.' : '아직 사고판 기록이 없어요.'}
            </Text>
          ) : (
            <>
              <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
                {byPeriod ? '이 기간' : '전체'} {visible.length}건
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
