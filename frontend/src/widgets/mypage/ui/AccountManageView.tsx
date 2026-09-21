import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { useMyAccount, useUnlinkAccount } from '@/features/account';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';
import { AccountCard } from '@/widgets/parent-home';

/**
 * 마이페이지 - 연동 계좌 관리 화면.
 * 상단에 계좌 정보 카드를 보여주고, 하단에는 '연동 계좌 변경' 및 '연동 계좌 해제' 설정 옵션을 제공한다.
 */
export function AccountManageView() {
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
    <Screen
      scroll
      header={<Header title="연동 계좌 관리" onBack={() => router.back()} />}
    >
      <View style={{ marginTop: theme.space.md, gap: theme.space['2xl'] }}>
        <AccountCard
          account={account.data}
          isPending={account.isPending}
          onLinkAccount={() => router.push('/account/link')}
          showActions={false}
        />

        <View style={{ gap: theme.space.xs }}>
          <Pressable
            onPress={() => router.push({ pathname: '/account/link', params: { mode: 'change' } })}
            accessibilityRole="button"
            style={[styles.menuRow, { paddingVertical: theme.space.lg }]}
          >
            <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
              연동 계좌 변경
            </Text>
            <Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />
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
              <Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />
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
