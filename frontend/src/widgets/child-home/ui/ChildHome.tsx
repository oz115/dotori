import { useState } from 'react';
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

/**
 * 자녀 홈 화면 본체. wallet·chore·store feature 를 조합하는 화면 배치용 widget.
 * 하단 탭바는 이 화면이 그리지 않는다 — `(child)/(tabs)/_layout.tsx` 의 `<Tabs>` 가 고정으로 갖고 있다.
 */
export function ChildHome() {
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
