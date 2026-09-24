import { router } from 'expo-router';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { AccountCard, useMyAccount, useUnlinkAccount } from '@/features/account';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type AccountManageViewProps = {
  /** 계좌 연동 화면으로 (아직 연동이 없을 때) */
  onLinkAccount: () => void;
  /**
   * 연동 계좌 변경 화면으로.
   *
   * 경로를 이 화면이 알지 않는다 - 부모는 /(parent)/account/link, 자녀는
   * /(child)/setup/account/link 로 서로 다르고, 짧게 "/account/link" 로 밀면 자녀 화면에서도
   * 부모 경로로 잡혀 아무 일도 일어나지 않았다 (QA - 자녀의 계좌 변경이 안 먹던 원인).
   */
  onChangeAccount: () => void;
};

/**
 * 마이페이지 - 연동 계좌 관리 화면.
 * 상단에 계좌 정보 카드를 보여주고, 하단에는 '연동 계좌 변경' 및 '연동 계좌 해제' 설정 옵션을 제공한다.
 */
export function AccountManageView({ onLinkAccount, onChangeAccount }: AccountManageViewProps) {
  const theme = useTheme();
  const account = useMyAccount();
  const unlinkAccount = useUnlinkAccount();

  const handleUnlink = () => {
    Alert.alert(
      '연동 계좌 해제',
      '연동을 해제하면 다시 계좌를 연결할 때까지 도토리 충전 및 환전 기능이 제한돼요. 정말 해제하시겠어요?',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '해제',
          style: 'destructive',
          onPress: () => {
            unlinkAccount.mutate(undefined, {
              onSuccess: () => {
                Alert.alert('완료', '계좌 연동이 해제되었어요.');
              },
              onError: () => {
                Alert.alert('오류', '계좌 해제에 실패했어요. 다시 시도해 주세요.');
              },
            });
          },
        },
      ],
    );
  };

  const isLinked = Boolean(account.data);

  return (
    <Screen scroll header={<Header title="연동 계좌 관리" onBack={() => router.back()} />}>
      <View style={{ marginTop: theme.space.md, gap: theme.space['2xl'] }}>
        <AccountCard
          account={account.data}
          isPending={account.isPending}
          onLinkAccount={onLinkAccount}
          showActions={false}
        />

        <View style={{ gap: theme.space.xs }}>
          <Pressable
            onPress={onChangeAccount}
            accessibilityRole="button"
            style={[styles.menuRow, { paddingVertical: theme.space.lg }]}
          >
            <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
              연동 계좌 변경
            </Text>
          </Pressable>

          {isLinked ? (
            <Pressable
              onPress={handleUnlink}
              accessibilityRole="button"
              disabled={unlinkAccount.isPending}
              style={[styles.menuRow, { paddingVertical: theme.space.lg }]}
            >
              <Text style={[theme.typography.titleMd, { color: theme.color.status.danger.text }]}>
                연동 계좌 해제
              </Text>
            </Pressable>
          ) : null}
        </View>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  menuRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
});
