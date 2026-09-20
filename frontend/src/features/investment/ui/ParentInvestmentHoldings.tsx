import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { useInvestmentHoldings } from '@/features/investment/model/useInvestmentHoldings';
import { HoldingCard } from '@/features/investment/ui/HoldingCard';
import { InvestmentQueryError } from '@/features/investment/ui/InvestmentQueryError';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type Props = {
  /** 없으면 부모 본인 보유를 묻게 되므로 조회하지 않는다 */
  childId: number | undefined;
  childName: string | undefined;
  onBack: () => void;
  onPressStock: (productId: number) => void;
};

/** 자녀가 들고 있는 주식 전체 (부모). 메인 요약 띠에는 앞 몇 개만 나온다. */
export function ParentInvestmentHoldings({ childId, childName, onBack, onPressStock }: Props) {
  const theme = useTheme();
  const holdings = useInvestmentHoldings(childId, { enabled: childId != null });
  const muted = [theme.typography.body, { color: theme.color.text.muted }];

  return (
    <Screen
      header={
        <Header title={childName ? `${childName}의 보유 주식` : '보유 주식'} onBack={onBack} />
      }
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
    >
      {childId == null ? (
        <View style={styles.center}>
          <Text style={muted}>어느 아이의 보유 주식인지 알 수 없어요.</Text>
        </View>
      ) : holdings.isPending || holdings.isError ? (
        <View style={[styles.center, { gap: theme.space.lg }]}>
          <Text style={muted}>
            {holdings.isPending ? '보유 주식을 불러오는 중이에요.' : '불러오지 못했어요.'}
          </Text>
          {holdings.isError ? (
            <InvestmentQueryError
              error={holdings.error}
              fetching={holdings.isFetching}
              onRetry={() => holdings.refetch()}
            />
          ) : null}
        </View>
      ) : holdings.data.length === 0 ? (
        <View style={styles.center}>
          <Text style={muted}>아직 산 주식이 없어요.</Text>
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
          {holdings.data.map((holding) => (
            <HoldingCard
              key={holding.id}
              holding={holding}
              onPress={() => onPressStock(holding.id)}
            />
          ))}
        </ScrollView>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
});
