import { router } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { useSavingsTransactions } from '@/features/savings/model/useSavingsTransactions';
import { SavingsTransactionCard } from '@/features/savings/ui/SavingsTransactionCard';
import { useTheme } from '@/shared/theme';
import { Button, Header, Screen } from '@/shared/ui';

type Props = {
  familyId: number | undefined;
  childId: number | undefined;
  /**
   * 헤더 제목. 부모가 볼 때는 누구 것인지 밝혀야 해서 이름을 넣어 넘긴다
   * ("천명석의 저축 거래 내역"). 아이 본인은 자기 것뿐이라 기본값으로 충분하다.
   */
  title?: string;
  /**
   * 뒤로가기 목적지. 안 넘기면 아이의 저축 탭으로 돌아간다.
   *
   * 부모는 반드시 넘겨야 한다 - `/save` 는 (child)·(parent) 두 그룹에 다 있어서
   * 그룹 접두사 없이 replace 하면 expo-router 가 어디로 갈지 못 정한다.
   */
  onBack?: () => void;
};

/**
 * 예적금 거래 이력 화면 — 납입·이자·수령·해지 기록을 최신순으로 보여준다.
 *
 * 아이는 저축 메인 헤더의 "거래 내역"에서, 부모는 "보유 현황"의 자녀 구역에서 들어온다.
 * 조회 API 가 (familyId, childId) 만 받으므로 역할별로 화면을 나누지 않고 같은 것을 쓴다.
 */
export function SavingsHistory({ familyId, childId, title, onBack }: Props) {
  const theme = useTheme();
  const transactions = useSavingsTransactions(familyId, childId);

  return (
    <Screen
      header={
        <Header
          title={title ?? '저축 거래 내역'}
          onBack={onBack ?? (() => (router.canGoBack() ? router.back() : router.replace('/save')))}
        />
      }
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
