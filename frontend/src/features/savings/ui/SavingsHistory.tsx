import { router } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { useSavingsTransactions } from '@/features/savings/model/useSavingsTransactions';
import { SavingsTransactionCard } from '@/features/savings/ui/SavingsTransactionCard';
import { useTheme } from '@/shared/theme';
import { Button, Header, Screen } from '@/shared/ui';

type Props = {
  familyId: number | undefined;
  childId: number | undefined;
};

/**
 * 예적금 거래 이력 화면 — 납입·이자·수령·해지 기록을 최신순으로 보여준다.
 * 저축 메인 헤더의 "거래 내역"에서 들어온다.
 */
export function SavingsHistory({ familyId, childId }: Props) {
  const theme = useTheme();
  const transactions = useSavingsTransactions(familyId, childId);

  return (
    <Screen
      header={
        <Header
          title="저축 거래 내역"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/save'))}
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
            <Button label="다시 시도" variant="outline" onPress={() => transactions.refetch()} />
          ) : null}
        </View>
      ) : transactions.data.length === 0 ? (
        <View style={styles.center}>
          <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
            아직 저축 기록이 없어요.
          </Text>
        </View>
      ) : (
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{
            paddingHorizontal: theme.layout.screenX,
            paddingVertical: theme.layout.screenY,
            paddingBottom: theme.space.lg,
            gap: theme.space.md,
          }}
        >
          {transactions.data.map((item) => (
            <SavingsTransactionCard key={item.id} item={item} />
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
