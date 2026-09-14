import { router } from 'expo-router';
import { Alert, ScrollView, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useMyAccount } from '@/features/account';
import { useTheme } from '@/shared/theme';
import { AppTopBar } from '@/shared/ui';

import { useParentFamily } from '@/widgets/parent-home/model/useParentFamily';

import { AccountCard } from './AccountCard';
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
  const { familyId, childId, childName, isPending: familyPending } = useParentFamily();
  const account = useMyAccount();

  const notReady = () => Alert.alert('준비 중이에요', '곧 만나볼 수 있어요.');

  // '/task', '/save', '/investment' 는 부모·자녀 그룹 모두에 있는 경로라
  // 그룹까지 적어야 부모 탭으로 간다
  const openChoreTab = () => router.push('/(parent)/(tabs)/task');
  const openSaveTab = () => router.push('/(parent)/(tabs)/save');
  const openInvestmentTab = () => router.push('/(parent)/(tabs)/investment');

  return (
    <SafeAreaView
      edges={['top']}
      style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}
    >
      <AppTopBar onProfilePress={() => router.push('/mypage')} />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space['2xl'] },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <ChildBalanceCard
          familyId={familyId}
          childId={childId}
          childName={childName}
          familyPending={familyPending}
          onCashout={openChoreTab}
        />

        <AccountCard
          account={account.data}
          isPending={account.isPending}
          onLinkAccount={() => router.push('/account/link')}
          onTransfer={notReady}
          onHistory={notReady}
        />

        <ApprovalsCard
          familyId={familyId}
          childId={childId}
          familyPending={familyPending}
          onOpenChores={openChoreTab}
          onOpenCashouts={openChoreTab}
        />

        <ChildAssetsSection onOpenSavings={openSaveTab} onOpenInvestment={openInvestmentTab} />

        <ChildTodayProgress
          familyId={familyId}
          childId={childId}
          familyPending={familyPending}
          onMorePress={openChoreTab}
        />

        <ChildActivityBoard />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  content: { paddingTop: 4, paddingBottom: 40 },
});
