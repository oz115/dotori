import { router } from 'expo-router';
import { ScrollView, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useMyAccount } from '@/features/account';
import { useWalletBalance } from '@/features/wallet';
import { useTheme } from '@/shared/theme';
import { AppTopBar } from '@/shared/ui';

import { useChildIdentity } from '@/widgets/child-home/model/useChildIdentity';

import { ActivityBoard } from './ActivityBoard';
import { BalanceCard } from './BalanceCard';
import { SavingGoals } from './SavingGoals';
import { TodayChores } from './TodayChores';
import { UpdateBanner } from './UpdateBanner';

/**
 * 자녀 홈 화면 본체. wallet·chore·store feature 를 조합하는 화면 배치용 widget.
 * 하단 탭바는 이 화면이 그리지 않는다 — `(child)/(tabs)/_layout.tsx` 의 `<Tabs>` 가 고정으로 갖고 있다.
 */
export function ChildHome() {
  const theme = useTheme();
  const { familyId, childId } = useChildIdentity();
  const balance = useWalletBalance(familyId, childId);
  const account = useMyAccount();

  return (
    <SafeAreaView
      edges={['top']}
      style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}
    >
      <AppTopBar onProfilePress={() => router.push('/mypage')} />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.space['2xl'], gap: theme.space['2xl'] },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <BalanceCard
          balance={balance.data?.balance}
          isPending={balance.isPending}
          onCharge={() => router.push('/wallet/charge')}
          onCashout={() => router.push('/wallet/cashout')}
          account={account.data}
        />
        <UpdateBanner familyId={familyId} childId={childId} />
        <TodayChores familyId={familyId} childId={childId} />
        <SavingGoals familyId={familyId} balance={balance.data?.balance} />
        <ActivityBoard />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  // 마지막 섹션(ActivityBoard)이 연회색 배경을 화면 아래 끝까지 깔아서 여기선 여백을 두지 않는다
  content: { paddingTop: 4, paddingBottom: 0 },
});
