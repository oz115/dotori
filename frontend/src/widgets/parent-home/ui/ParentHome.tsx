import { router } from 'expo-router';
import { ScrollView, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AccountCard, useMyAccount } from '@/features/account';
import { useUnreadNotificationCount } from '@/features/notification';
import { useTheme } from '@/shared/theme';
import { AppTopBar, PagedCards, usePullToRefresh } from '@/shared/ui';

import { useParentFamily } from '@/widgets/parent-home/model/useParentFamily';

import { ApprovalsCard } from './ApprovalsCard';
import { ChildActivityBoard } from './ChildActivityBoard';
import { ChildAssetsSection } from './ChildAssetsSection';
import { ChildBalanceCard } from './ChildBalanceCard';
import { ChildTodayProgress } from './ChildTodayProgress';

/**
 * 부모 홈 화면 본체. 자녀 홈이 "내 자산 + 내 할 일"이라면 이쪽은
 * "내가 처리해야 할 것 + 아이 현황"이다.
 *
 * 각 섹션은 요약만 보여주고 자세한 목록은 하단 탭 화면으로 넘긴다.
 * 하단 탭바는 `(parent)/(tabs)/_layout.tsx` 의 `<Tabs>` 가 갖고 있다.
 */
export function ParentHome() {
  const theme = useTheme();
  const refreshControl = usePullToRefresh();
  const { familyId, childId, childName, isPending: familyPending } = useParentFamily();
  const account = useMyAccount();
  const unreadCount = useUnreadNotificationCount();

  // '/task', '/save', '/investment', '/notifications' 는 부모·자녀 그룹 모두에 있는
  // 경로라 그룹까지 적어야 부모 쪽으로 간다
  const openChoreTab = () => router.push('/(parent)/(tabs)/task');
  const openCashouts = () => router.push('/(parent)/wallet/cashouts');
  const openSaveTab = () => router.push('/(parent)/(tabs)/save');
  const openInvestmentTab = () => router.push('/(parent)/(tabs)/investment');
  // 리포트는 탭이 아니라 푸시 라우트다 — 부모 탭이 다섯 개로 차 있다
  const openReport = () => router.push('/(parent)/report');
  // '/word' 도 아이 그룹에 같은 경로가 있어 그룹까지 적어야 부모용 단어장으로 간다
  const openWords = () => router.push('/(parent)/word');
  // '/quiz' 도 아이 그룹에 같은 경로가 있어 그룹까지 적어야 부모용 퀴즈 화면으로 간다
  const openQuiz = () => router.push('/(parent)/quiz');

  return (
    <SafeAreaView
      edges={['top']}
      style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}
    >
      <AppTopBar
        onProfilePress={() => router.push('/mypage')}
        onNotificationsPress={() => router.push('/(parent)/notifications')}
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
        {/* 옆으로 넘기면 부모 본인 계좌가 나온다 — 자녀 홈 지갑과 같은 슬라이드 */}
        <PagedCards
          pages={[
            <ChildBalanceCard
              key="child"
              familyId={familyId}
              childId={childId}
              childName={childName}
              familyPending={familyPending}
              onGrant={() => router.push('/(parent)/wallet/grant')}
              onReviewCashouts={openCashouts}
            />,
            <AccountCard
              key="account"
              fill
              account={account.data}
              isPending={account.isPending}
              onLinkAccount={() => router.push('/account/link')}
              onTransfer={() => router.push('/account/transfer')}
              onHistory={() => router.push('/account/transactions')}
            />,
          ]}
        />

        <ApprovalsCard
          familyId={familyId}
          childId={childId}
          familyPending={familyPending}
          onOpenChores={openChoreTab}
          onOpenCashouts={openCashouts}
        />

        <ChildAssetsSection
          familyId={familyId}
          childId={childId}
          familyPending={familyPending}
          onOpenSavings={openSaveTab}
          onOpenInvestment={openInvestmentTab}
          onOpenReport={openReport}
        />

        <ChildTodayProgress
          familyId={familyId}
          childId={childId}
          familyPending={familyPending}
          onMorePress={openChoreTab}
          onChorePress={(chore) =>
            router.push({
              pathname: '/task/[choreId]',
              // from=home: 상세의 뒤로가기가 목록이 아니라 홈으로 오게 한다.
              params: {
                choreId: String(chore.choreId),
                childId: String(chore.childId),
                from: 'home',
              },
            })
          }
        />

        <ChildActivityBoard
          familyId={familyId}
          childId={childId}
          onOpenQuiz={openQuiz}
          onOpenWords={openWords}
        />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  // 마지막 활동 보드의 배경이 둥근 탭바 뒤까지 이어진다.
  content: { paddingTop: 4, paddingBottom: 0 },
});
