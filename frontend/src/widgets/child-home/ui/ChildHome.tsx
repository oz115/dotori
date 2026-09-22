import { router } from 'expo-router';
import { ScrollView, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useMyAccount } from '@/features/account';
import { useUnreadNotificationCount } from '@/features/notification';
import { useWalletBalance } from '@/features/wallet';
import { useTheme } from '@/shared/theme';
import { AppTopBar, usePullToRefresh } from '@/shared/ui';

import { useChildAssets } from '@/widgets/child-home/model/useChildAssets';
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
  const refreshControl = usePullToRefresh();
  const { familyId, childId } = useChildIdentity();
  const balance = useWalletBalance(familyId, childId);
  const account = useMyAccount();
  const assets = useChildAssets(familyId, childId);
  const unreadCount = useUnreadNotificationCount();

  return (
    <SafeAreaView
      edges={['top']}
      style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}
    >
      <AppTopBar
        onProfilePress={() => router.push('/mypage')}
        onNotificationsPress={() => router.push('/(child)/notifications')}
        unreadCount={unreadCount.data?.unreadCount}
      />

      <ScrollView
        refreshControl={refreshControl}
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space['2xl'] },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <BalanceCard
          balance={balance.data?.balance}
          isPending={balance.isPending}
          onCharge={() => router.push('/wallet/charge')}
          onCashout={() => router.push('/wallet/cashout')}
          // 포인트 입출금·환전을 한 화면에서 보는 거래 내역으로 간다
          onCashoutHistory={() => router.push('/(child)/wallet/transactions')}
          account={account.data}
          assets={assets}
        />
        <UpdateBanner familyId={familyId} childId={childId} />
        <TodayChores
          familyId={familyId}
          childId={childId}
          onMorePress={() => router.push('/task')}
        />
        <SavingGoals
          familyId={familyId}
          balance={balance.data?.balance}
          onPressGoal={(productId) =>
            router.push({
              pathname: '/(child)/(tabs)/store/[productId]',
              // viaDeepLink: 홈(다른 탭)에서 store 탭 상세로 곧장 넘어왔다는 표시.
              // 상세 화면이 이걸 보고 목록을 먼저 깔아준다.
              params: { productId: String(productId), viaDeepLink: '1' },
            })
          }
        />
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
