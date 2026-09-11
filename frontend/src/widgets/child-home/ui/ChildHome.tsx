import { useState, type ReactNode } from 'react';
import { Alert, Modal, Pressable, ScrollView, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { signOut, WithdrawButton } from '@/features/auth';
import { useWalletBalance } from '@/features/wallet';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

import { useChildIdentity } from '@/widgets/child-home/model/useChildIdentity';

import { ActivityBoard } from './ActivityBoard';
import { BalanceCard } from './BalanceCard';
import { SavingGoals } from './SavingGoals';
import { TodayChores } from './TodayChores';
import { TopBar } from './TopBar';
import { UpdateBanner } from './UpdateBanner';

type ChildHomeProps = {
  /** 하단 탭바 (widgets/main-tab-bar). widget 끼리는 직접 import 하지 않아서 호출하는
   * app 레이어(라우트 파일)가 조립해 넘겨준다. */
  tabBar: ReactNode;
};

/**
 * 자녀 홈 화면 본체. wallet·chore·store feature 를 조합하는 화면 배치용 widget.
 */
export function ChildHome({ tabBar }: ChildHomeProps) {
  const theme = useTheme();
  const { familyId, childId } = useChildIdentity();
  const balance = useWalletBalance(familyId, childId);
  const [accountSheetOpen, setAccountSheetOpen] = useState(false);

  const notReady = () => Alert.alert('준비 중이에요', '곧 만나볼 수 있어요.');

  return (
    <SafeAreaView
      edges={['top']}
      style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}
    >
      <TopBar onProfilePress={() => setAccountSheetOpen(true)} />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space['2xl'] },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <BalanceCard
          balance={balance.data?.balance}
          isPending={balance.isPending}
          onCharge={notReady}
          onCashout={notReady}
        />
        <UpdateBanner familyId={familyId} childId={childId} />
        <TodayChores familyId={familyId} childId={childId} />
        <SavingGoals familyId={familyId} balance={balance.data?.balance} />
        <ActivityBoard />
      </ScrollView>

      {/* 투자 등 다른 탭으로는 하단 탭바로 이동한다. bottom safe-area 는 탭바가 처리 */}
      {tabBar}

      <AccountSheet visible={accountSheetOpen} onClose={() => setAccountSheetOpen(false)} />
    </SafeAreaView>
  );
}

type AccountSheetProps = {
  visible: boolean;
  onClose: () => void;
};

/**
 * 로그아웃·회원 탈퇴 임시 시트. 마이페이지가 생기기 전까지 프로필 아이콘 뒤에 붙여둔다.
 */
function AccountSheet({ visible, onClose }: AccountSheetProps) {
  const theme = useTheme();

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose}>
        <Pressable
          style={[
            styles.sheet,
            {
              backgroundColor: theme.color.background.canvas,
              borderRadius: theme.radius.lg,
              padding: theme.space.xl,
              gap: theme.space.sm,
            },
          ]}
          onPress={(e) => e.stopPropagation()}
        >
          <Button
            label="로그아웃"
            variant="ghost"
            onPress={() => {
              onClose();
              void signOut();
            }}
          />
          <WithdrawButton />
        </Pressable>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  content: { paddingTop: 4, paddingBottom: 40 },
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'flex-end',
  },
  sheet: {},
});
