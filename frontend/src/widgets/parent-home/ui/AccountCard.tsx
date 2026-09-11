import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { AccountInfo } from '@/features/account';
import { formatWonNumber } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';

type AccountCardProps = {
  account: AccountInfo | undefined;
  isPending: boolean;
  /** 계좌가 없을 때 연동 화면으로 */
  onLinkAccount: () => void;
};

/**
 * 부모 본인 계좌 카드. 자녀 도토리 카드(초록) 아래에 오며, 성격이 다른 돈이라
 * 도토리 브라운(palette.acorn)으로 구분한다 — 위는 아이의 포인트, 여기는 내 현금.
 *
 * 이 계좌는 충전(자녀→부모)의 수취 계좌이자 환전 승인(부모→자녀)의 출금 계좌라서,
 * 연동돼 있지 않으면 아이의 충전·환전이 서버에서 막힌다. 그래서 없을 때를 눈에 띄게 안내한다.
 */
export function AccountCard({ account, isPending, onLinkAccount }: AccountCardProps) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: theme.color.accent.acorn,
          borderRadius: theme.radius.lg,
          padding: theme.space.xl,
          gap: theme.space.md,
        },
      ]}
    >
      <View style={styles.row}>
        <View style={{ flex: 1, gap: theme.space.xs }}>
          <Text
            style={[theme.typography.caption, { color: theme.color.text.onBrand, opacity: 0.85 }]}
          >
            내 계좌{account ? ` · ${account.bankName}` : ''}
          </Text>

          {isPending ? (
            <Text style={[theme.typography.h2, { color: theme.color.text.onBrand }]}>—</Text>
          ) : account ? (
            <Text style={[theme.typography.h2, { color: theme.color.text.onBrand }]}>
              {formatWonNumber(account.accountBalance)}원
            </Text>
          ) : (
            <Text style={[theme.typography.body, { color: theme.color.text.onBrand }]}>
              아직 연동된 계좌가 없어요
            </Text>
          )}
        </View>

        {account ? (
          <Ionicons name="wallet-outline" size={28} color={theme.color.text.onBrand} />
        ) : (
          <Pressable
            onPress={onLinkAccount}
            style={[
              styles.linkButton,
              {
                backgroundColor: theme.color.background.surface,
                borderRadius: theme.radius.sm,
                paddingHorizontal: theme.space.md,
              },
            ]}
          >
            <Text style={[theme.typography.label, { color: theme.color.accent.acorn }]}>
              계좌 연동
            </Text>
          </Pressable>
        )}
      </View>

      {!isPending && !account ? (
        <Text
          style={[theme.typography.caption, { color: theme.color.text.onBrand, opacity: 0.85 }]}
        >
          계좌를 연결해야 아이가 도토리를 충전하고 환전할 수 있어요.
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {},
  row: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  linkButton: { minHeight: 40, alignItems: 'center', justifyContent: 'center' },
});
